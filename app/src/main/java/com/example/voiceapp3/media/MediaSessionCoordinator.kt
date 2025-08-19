package com.example.voiceapp3.media

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.voiceapp3.car.MediaCenterBridge
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit


class MediaSessionCoordinator(
    var context: Context,
    private val gateway: MediaCenterBridge
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentController: MediaController? = null
    private var callbackHandler: MediaControllerCallbackHandler? = null
    private var progressJob: Job? = null

    private companion object {
        const val DEBOUNCE_DELAY_MS = 300L
    }

    private var lastControllerChangeTime = 0L
    private val observer = MediaSessionObserver(context) { controller ->
        onNewController(controller)
    }

    fun start() {
        observer.start()
        checkCurrentSessions()
        Log.i("MediaCoordinator", "Started listening")
    }

    private fun isPlayingState(state: Int): Boolean {
        return state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING
    }

    private fun getSessionPriority(controller: MediaController): Int {
        val state = controller.playbackState?.state ?: PlaybackState.STATE_NONE
        return when {
            isPlayingState(state) -> 0  // Highest priority - currently playing
            state != PlaybackState.STATE_NONE -> 1  // Active but not playing
            else -> 2  // Inactive sessions
        }
    }

    private fun checkCurrentSessions() {
        val now = System.currentTimeMillis()
        if (now - lastControllerChangeTime < DEBOUNCE_DELAY_MS) {
            return
        }

        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val activeSessions = mediaSessionManager.getActiveSessions(null)

        // Filter out current session if we're not starting
        val filteredSessions = activeSessions.filterNot { it.packageName == currentController?.packageName }

        if (filteredSessions.isEmpty()) {
            if (currentController != null) {
                onNewController(null)
            }
            return
        }

        // Sort sessions by priority
        val sortedSessions = filteredSessions.sortedBy { getSessionPriority(it) }

        // Only notify if the top session is different from current
        val newTopSession = sortedSessions.firstOrNull()
        if (newTopSession?.packageName != currentController?.packageName) {
            lastControllerChangeTime = now
            observer.onActiveSessionsChanged(sortedSessions.toMutableList())
        }
    }

    fun stop() {
        observer.stop()
        progressJob?.cancel()
        scope.cancel()
        Log.d("MediaCoordinator", "Stopped listening")
    }

    private fun onNewController(controller: MediaController?) {
        Log.i("MediaCoordinator", "onNewController called with ${controller?.packageName}")
        Log.i("MediaCoordinator", "Current controller: ${currentController?.packageName}")

        // Special case: If we're getting a null controller but have a valid current one,
        // this might be a session death - we should keep our current controller
        if (controller == null && currentController != null) {
            Log.i("MediaCoordinator", "Ignoring null controller while we have active session")
            return
        }

        // Special case: If the new controller is invalid (no metadata/playback state)
        // and we have a valid current controller, ignore it
        if (controller != null && currentController != null &&
            (controller.metadata == null || controller.playbackState == null)) {
            Log.i("MediaCoordinator", "Ignoring invalid controller while we have active session")
            return
        }

        // Skip if this is the same controller we're already handling
        if (controller?.packageName == currentController?.packageName) {
            checkCurrentSessions()
            return
        }

        Log.i("MediaCoordinator", "Proceeding with controller change to ${controller?.packageName}")

        cleanupCurrentController()
        progressJob?.cancel()
        gateway.unregisterPlayer()

        if (controller == null) {
            return
        }

        try {
            // Additional validation before switching
            if (controller.metadata == null && controller.playbackState == null) {
                Log.w("MediaCoordinator", "Rejecting controller with no metadata or playback state")
                return
            }

            val handler = MediaControllerCallbackHandler(controller, gateway, ::cleanupCurrentController)
            controller.registerCallback(handler, Handler(Looper.getMainLooper()))

            currentController = controller
            callbackHandler = handler

            val info = MetadataAdapter.toPlaybackInfo(controller, context)
            gateway.registerPlayer(controller.packageName)
            gateway.updateWithPlaybackInfo(info.apply { playbackStatus = 1 })

            if (isPlayingState(controller.playbackState?.state ?: PlaybackState.STATE_NONE)) {
                startProgressLoop(controller)
            }
        } catch (e: Exception) {
            Log.e("MediaCoordinator", "Error handling new controller", e)
            cleanupCurrentController()
        }
    }

    private fun cleanupCurrentController() {
        callbackHandler?.let { handler ->
            currentController?.unregisterCallback(handler)
        }
        currentController = null
        callbackHandler = null
    }

    private fun startProgressLoop(controller: MediaController) {
        progressJob = scope.launch {
            while (isActive && controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                gateway.updateProgress(controller.playbackState?.position ?: return@launch)
                delay(100)
            }
        }
    }

    fun cleanExpiredCache() {
        val ttl = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        listOfNotNull(
            context.externalCacheDir,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        ).forEach { base ->
            File(base, "art").listFiles()
                ?.filter { it.isFile && it.lastModified() < ttl }
                ?.forEach { it.delete() }
        }
    }
}