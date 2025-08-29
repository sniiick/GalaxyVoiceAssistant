package com.example.voiceapp3.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Environment
import android.os.Handler
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.example.voiceapp3.car.MediaCenterBridge
import ecarx.xsf.mediacenter.MusicPlaybackInfo
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MediaSessionCoordinator(
    var context: Context,
    private val gateway: MediaCenterBridge
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentController: MediaController? = null
    private var callbackHandler: MediaControllerCallbackHandler? = null
    private var progressJob: Job? = null
    private var lastPosition: Long = 0
    private var positionUpdateTime: Long = 0

    private var lastNativeSessionDetectionTime: Long = 0
    private var nativeSessionCooldownPeriod: Long = 5000
    private var lastNativeSessionPackage: String? = null

    private var currentSessionPackage: String? = null
    private var pollingJob: Job? = null
    private val nativePackages = setOf(
        "com.android.bluetooth",
        "com.ecarx.mediacenter",
        "com.flyme"
    )

    fun start() {
        startPolling()
        Log.i("MediaCoordinator", "Started polling media sessions")
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                pollMediaSessions()
                delay(1000) // Poll every second
            }
        }
    }

    private fun pollMediaSessions() {
        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val activeSessions = mediaSessionManager.getActiveSessions(null)

        if (activeSessions.isEmpty()) {
            cleanupCurrentController()
            return
        }

        // Check if we're in a cooldown period for native session detection
        val currentTime = SystemClock.elapsedRealtime()
        val inCooldown = currentTime - lastNativeSessionDetectionTime < nativeSessionCooldownPeriod

        if (inCooldown && lastNativeSessionPackage == "com.android.bluetooth") {
            Log.d("MediaCoordinator", "In cooldown period for Bluetooth detection, skipping native check")
            // Continue with non-native session logic during cooldown
            handleNonNativeSessions(activeSessions)
            return
        }

        // Find ALL playing sessions (both native and non-native)
        val playingSessions = activeSessions.filter { isActuallyPlaying(it) }

        // Check if there are any NATIVE playing sessions
        val nativePlayingSession = playingSessions.firstOrNull { isNativeSession(it) }

        if (nativePlayingSession != null) {
            // Native session detected - enter cooldown period
            lastNativeSessionDetectionTime = currentTime
            lastNativeSessionPackage = nativePlayingSession.packageName

            Log.i("MediaCoordinator", "Native session ${nativePlayingSession.packageName} detected, entering cooldown period")
            cleanupCurrentController()
            return
        }

        // No native sessions playing, handle non-native sessions
        handleNonNativeSessions(activeSessions)
    }

    private fun handleNonNativeSessions(activeSessions: List<MediaController>) {
        val playingSessions = activeSessions.filter { isActuallyPlaying(it) && !isNativeSession(it) }
        val nonNativePlayingSession = playingSessions.firstOrNull()

        if (nonNativePlayingSession != null) {
            // Non-native session is playing
            if (nonNativePlayingSession.packageName != currentController?.packageName) {
                Log.i("MediaCoordinator", "Switching to playing session: ${nonNativePlayingSession.packageName}")
                switchToController(nonNativePlayingSession)
            } else {
                // Same non-native session is still playing
                updateCurrentSessionState()
            }
            return
        }

        // No playing sessions found at all
        val currentSession = currentController
        if (currentSession != null && isSessionActive(currentSession)) {
            // Current session is active but paused - keep it, just update playback status
            Log.d("MediaCoordinator", "Current session ${currentSession.packageName} is paused but active")
            progressJob?.cancel()
        } else {
            // No valid sessions at all, cleanup
            Log.d("MediaCoordinator", "No valid sessions found, cleaning up")
            gateway.updateWithPlaybackInfo(MusicPlaybackInfo().apply { playbackStatus = 2 })
            cleanupCurrentController()
        }
    }

    private fun updateCurrentSessionState() {
        val current = currentController ?: return
        val playbackState = current.playbackState ?: return

        if (isPlayingState(playbackState.state)) {
            // Session is playing, ensure progress updates are running
            if (progressJob?.isActive != true) {
                startProgressLoop(current)
            }

            // Update playback status to playing (in case it was paused before)
            val info = MetadataAdapter.toPlaybackInfo(current, context)
            gateway.updateWithPlaybackInfo(info.apply {
                playbackStatus = 1 // Playing
            })
        } else if (isSessionActive(current)) {
            // Session is paused but still active
            progressJob?.cancel()
        } else {
            // Session is no longer active, cleanup
            Log.i("MediaCoordinator", "Current session ${current.packageName} is no longer active")
            cleanupCurrentController()
        }
    }

    private fun isNativeSession(controller: MediaController): Boolean {
        return nativePackages.any { native ->
            controller.packageName?.startsWith(native) == true
        }
    }

    private fun isPlayingState(state: Int): Boolean {
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    }

    private fun isActuallyPlaying(session: MediaController): Boolean {
        val playbackState = session.playbackState ?: return false
        val metadata = session.metadata ?: return false

        // Must be in playing state (3=playing, 6=buffering)
        if (!isPlayingState(playbackState.state)) {
            return false
        }

        // Must have valid metadata (title or artist)
        val hasValidMetadata = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotEmpty() == true ||
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.isNotEmpty() == true

        if (!hasValidMetadata) {
            return false
        }

        // For native sessions (especially Bluetooth), be more strict about position changes
        if (isNativeSession(session)) {
            return isNativeSessionActuallyPlaying(session, playbackState)
        }

        // For non-native sessions, trust the playing state
        return true
    }

    private fun isNativeSessionActuallyPlaying(session: MediaController, playbackState: PlaybackState): Boolean {
        // Only check Bluetooth sessions, be more permissive with other native sessions
        if (session.packageName != "com.android.bluetooth") {
            val currentTime = SystemClock.elapsedRealtime()
            val lastUpdateTime = playbackState.lastPositionUpdateTime
            val isRecentlyUpdated = currentTime - lastUpdateTime < 1500
            val hasNonZeroPosition = playbackState.position > 0
            return isRecentlyUpdated && hasNonZeroPosition
        }

        // For Bluetooth, be very conservative
        val currentPosition = playbackState.position
        val currentTime = SystemClock.elapsedRealtime()
        val lastUpdateTime = playbackState.lastPositionUpdateTime

        Log.d("MediaControllerCallback", "Bluetooth check: pos=$currentPosition, lastUpdate=$lastUpdateTime, current=$currentTime, diff=${currentTime - lastUpdateTime}")

        // Bluetooth must show significant position changes to be considered playing
        if (session.packageName == currentSessionPackage) {
            val positionDiff = abs(currentPosition - lastPosition)

            // Require at least 500ms position change to consider it real playback
            if (positionDiff < 500) {
                Log.d("MediaControllerCallback", "Bluetooth position change too small ($positionDiff ms), not real playback")
                return false
            } else {
                lastPosition = currentPosition
                positionUpdateTime = currentTime
                Log.d("MediaControllerCallback", "Bluetooth significant position change ($positionDiff ms), considering playing")
                return true
            }
        } else {
            // New session, reset tracking but be skeptical
            lastPosition = currentPosition
            positionUpdateTime = currentTime
            currentSessionPackage = session.packageName
        }

        // Additional check: must have very recent updates for Bluetooth
        val timeSinceUpdate = currentTime - lastUpdateTime
        if (timeSinceUpdate > 2000) {
            Log.d("MediaControllerCallback", "Bluetooth no recent updates for $timeSinceUpdate ms")
            return false
        }

        // If we're still not sure, be conservative and don't trust Bluetooth
        Log.d("MediaControllerCallback", "Bluetooth detection uncertain, not trusting it")
        return false
    }

    private fun isSessionActive(session: MediaController): Boolean {
        val playbackState = session.playbackState ?: return false
        val metadata = session.metadata ?: return false

        // Session must have valid metadata (title or artist)
        val hasValidMetadata = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotEmpty() == true ||
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.isNotEmpty() == true

        if (!hasValidMetadata) {
            return false
        }

        // Session is active if it's in any state except stopped, error, or none
        val isActiveState = playbackState.state != PlaybackState.STATE_STOPPED &&
                playbackState.state != PlaybackState.STATE_ERROR &&
                playbackState.state != PlaybackState.STATE_NONE

        // Also check if the session package is still in the active sessions list
        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val activeSessionPackages = mediaSessionManager.getActiveSessions(null).map { it.packageName }

        return isActiveState && session.packageName in activeSessionPackages
    }

    private fun switchToController(controller: MediaController) {
        cleanupCurrentController()

        try {
            val handler = MediaControllerCallbackHandler(controller, gateway) {
                // Session destroyed - will be handled in next poll
            }

            controller.registerCallback(handler, Handler(context.mainLooper))
            currentController = controller
            callbackHandler = handler

            val info = MetadataAdapter.toPlaybackInfo(controller, context)
            gateway.registerPlayer(controller.packageName)
            gateway.updateWithPlaybackInfo(info.apply { playbackStatus = 1 })

            startProgressLoop(controller)
        } catch (e: Exception) {
            Log.e("MediaCoordinator", "Error switching to controller", e)
            cleanupCurrentController()
        }
    }

    private fun cleanupCurrentController() {
        callbackHandler?.let { handler ->
            currentController?.unregisterCallback(handler)
        }
        currentController = null
        callbackHandler = null
        progressJob?.cancel()
        gateway.unregisterPlayer()
    }

    private fun startProgressLoop(controller: MediaController) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val playbackState = controller.playbackState
                if (playbackState != null && isPlayingState(playbackState.state)) {
                    gateway.updateProgress(playbackState.position)
                }
                delay(100) // Update progress every 100ms
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        progressJob?.cancel()
        scope.cancel()
        cleanupCurrentController()
        Log.d("MediaCoordinator", "Stopped polling")
    }

    fun cleanExpiredCache() {
        val ttl = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
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