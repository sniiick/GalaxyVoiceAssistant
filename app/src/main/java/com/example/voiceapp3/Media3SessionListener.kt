package com.example.voiceapp3


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.voiceapp3.car.MediaCenterBridge
import ecarx.xsf.mediacenter.MusicPlaybackInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class Media3SessionListener(private val context: Context, private val bridge: MediaCenterBridge) :
    MediaSessionManager.OnActiveSessionsChangedListener {

    private val handler = Handler(Looper.getMainLooper())
    private var currentController: MediaController? = null
    private var progressUpdateRunnable: Runnable? = null
    private var isPlaying = false
    private var sessionManager: MediaSessionManager? = null
    private var lastActivePackage: String? = null
    private var lastStateChangeTime: Long = 0
    private val MIN_STATE_CHANGE_DELAY = 300

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.i("MediaSession", "Metadata changed: ${metadata?.description?.title}")
            metadata?.let {
                updateBridgeWithMetadata(
                    it,
                    currentController?.packageName ?: "ru.yandex.music",
                    if (isPlaying) 1 else 0)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.i("MediaSession", "PlaybackState changed: ${state?.state}, $state.")
            super.onPlaybackStateChanged(state)
            state?.let {
                val newIsPlaying = when (state.state) {
                    PlaybackState.STATE_PLAYING -> true
                    PlaybackState.STATE_PAUSED,
                    PlaybackState.STATE_STOPPED,
                    PlaybackState.STATE_ERROR -> false
                    else -> isPlaying
                }

                if (newIsPlaying != isPlaying) {
                    isPlaying = newIsPlaying
                    currentController?.metadata?.let { metadata ->
                        updateBridgeWithMetadata(
                            metadata,
                            currentController?.packageName ?: "ru.yandex.music",
                            if (isPlaying) 1 else 0
                        )
                    }
                    handleProgressUpdates(newIsPlaying)
                }

                if (isPlaying) {
                    updateProgress(state)
                }
            }
        }

        override fun onSessionDestroyed() {
            Log.i("MediaSession", "Session destroyed")
            cleanupCurrentController()
        }
    }

    fun startListening() {
        sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val notificationListener = ComponentName(context, VoiceAssistantService::class.java)

        try {
            sessionManager?.addOnActiveSessionsChangedListener(this, notificationListener, handler)
            val currentSessions = sessionManager?.getActiveSessions(notificationListener)
            onActiveSessionsChanged(currentSessions)
        } catch (e: SecurityException) {
            Log.e("MediaSession", "MEDIA_CONTENT_CONTROL permission required", e)
        } catch (e: Exception) {
            Log.e("MediaSession", "Error starting listener", e)
        }
    }

    fun stopListening() {
        sessionManager?.let {
            it.removeOnActiveSessionsChangedListener(this)
            cleanupCurrentController()
            sessionManager = null
        }
    }

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        val now = System.currentTimeMillis()
        if (now - lastStateChangeTime < MIN_STATE_CHANGE_DELAY) {
            return // Skip rapid successive changes
        }

        val newController = getMostRelevantController(controllers)
        val newPackage = newController?.packageName

        // Determine if this is a real session change
        val isRealChange = when {
            newController == null -> currentController != null
            currentController == null -> true
            newPackage != currentController?.packageName -> true
            else -> hasSignificantStateChange(newController)
        }

        if (isRealChange) {
            lastStateChangeTime = now
            handleControllerChange(newController, newPackage)
        }
    }

    private fun hasSignificantStateChange(newController: MediaController): Boolean {
        val oldState = currentController?.playbackState?.state ?: return true
        val newState = newController.playbackState?.state ?: return true

        return when {
            // Becoming active from inactive
            newState == PlaybackState.STATE_PLAYING && oldState != PlaybackState.STATE_PLAYING -> true
            // Session was paused but now is playing
            newState == PlaybackState.STATE_PLAYING && oldState == PlaybackState.STATE_PAUSED -> true
            // Session was stopped but now is active
            newState != PlaybackState.STATE_STOPPED && oldState == PlaybackState.STATE_STOPPED -> true
            else -> false
        }
    }

    private fun handleControllerChange(newController: MediaController?, newPackage: String?) {
        logSessionChange(newController, "Before change")

        cleanupCurrentController()
        newController?.let { controller ->
            currentController = controller
            lastActivePackage = newPackage
            controller.registerCallback(controllerCallback, handler)
            updateFromController(controller)
        }

        logSessionChange(newController, "After change")
    }

    private fun getMostRelevantController(controllers: List<MediaController>?): MediaController? {
        return controllers?.maxWithOrNull(compareBy(
            { it.playbackState?.state == PlaybackState.STATE_PLAYING },
            { it.playbackState?.lastPositionUpdateTime ?: 0L }
        ))
    }

    private fun logSessionChange(controller: MediaController?, reason: String) {
        Log.i("MediaSession", """
            $reason
            Current: ${currentController?.packageName} (${currentController?.playbackState?.state})
            New: ${controller?.packageName} (${controller?.playbackState?.state})
            Last Active: $lastActivePackage
        """.trimIndent())
    }

    private fun updateFromController(controller: MediaController) {
        val state = controller.playbackState?.state ?: PlaybackState.STATE_NONE
        isPlaying = state == PlaybackState.STATE_PLAYING

        controller.metadata?.let { metadata ->
            updateBridgeWithMetadata(
                metadata,
                controller.packageName,
                if (isPlaying) 1 else 0
            )
        }

        if (isPlaying) {
            handleProgressUpdates(true)
        }
    }

    private fun cleanupCurrentController() {
        currentController?.unregisterCallback(controllerCallback)
        progressUpdateRunnable?.let { handler.removeCallbacks(it) }
        currentController = null
    }

    private fun handleProgressUpdates(shouldUpdate: Boolean) {
        progressUpdateRunnable?.let { handler.removeCallbacks(it) }
        progressUpdateRunnable = null

        if (shouldUpdate) {
            progressUpdateRunnable = object : Runnable {
                override fun run() {
                    currentController?.playbackState?.let { state ->
                        if (state.state == PlaybackState.STATE_PLAYING) {
                            updateProgress(state)
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
            }.also { handler.post(it) }
        }
    }

    private fun updateProgress(state: PlaybackState) {
        val position = state.position
        bridge.updateProgress(position)
    }

    private fun handleArtwork(metadata: MediaMetadata): Uri {
        // 1. First try to get URI from string
        val uriString = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)

        uriString?.let {
            return try {
                it.toUri().normalizeScheme()
            } catch (_: Exception) {
                Log.w("Artwork", "Invalid URI format: $it")
            } as Uri
        }

        // 2. Fall back to Bitmap if URI string not available
        val artworkBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        return artworkBitmap?.let { bitmap ->
            convertBitmapToUri(context, bitmap)
        } ?: Uri.EMPTY
    }

    @SuppressLint("SetWorldReadable")
    private fun convertBitmapToUri(context: Context, bitmap: Bitmap): Uri {
        return try {
            val cacheDir = File(context.externalCacheDir, "artwork").apply {
                if (!exists()) mkdirs()
            }

            val file = File(cacheDir, "artwork_${System.currentTimeMillis()}.jpg").apply {
                FileOutputStream(this).use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)) {
                        throw IOException("Failed to compress bitmap")
                    }
                }
                setReadable(true, false)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            context.grantUriPermission(
                "com.flyme.auto.mediacontrol",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.grantUriPermission(
                "com.android.systemui",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            return createPublicUri(bitmap)
        } catch (e: Exception) {
            Log.e("Artwork", "Failed to convert bitmap to URI", e)
            Uri.EMPTY
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun createPublicUri(bitmap: Bitmap): Uri {
        val publicDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "voiceapp")

        publicDir.mkdirs()

        val file = File(publicDir, "artwork_${System.currentTimeMillis()}.jpg").apply {
            FileOutputStream(this).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }
            setReadable(true, false)
        }

        return Uri.fromFile(file)
    }

    fun getLaunchPendingIntent(): PendingIntent? {
        return try {
            currentController?.sessionActivity
        } catch (e: Exception) {
            Log.e("MediaSession", "Error getting launch PendingIntent", e)
            null
        }
    }

    fun getPlayerPendingIntent(): PendingIntent? {
        return try {
            currentController?.extras?.getParcelable("android.media.session.MediaButtonReceiver")
        } catch (e: Exception) {
            Log.e("MediaSession", "Error getting player PendingIntent", e)
            null
        }
    }

    private fun updateBridgeWithMetadata(metadata: MediaMetadata?, packageName: String = "ru.yandex.music", playbackStatus: Int) {
        val playbackInfo = MusicPlaybackInfo(context).apply {
            setIconPackage(packageName)
            setAppName("YandexMusic")
            setPackageName("ru.yandex.music")
            setPlaybackStatus(playbackStatus)
            setSourceType(6)

            metadata?.let {
                // Handle both standard and Media3 metadata fields
                val duration = sequenceOf(
                    MediaMetadata.METADATA_KEY_DURATION,
                    "android.media.metadata.DURATION"
                ).firstNotNullOfOrNull { metadata.getLong(it) }
                duration?.let { setDuration(it) }

                val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: metadata.description.title?.toString()
                title?.let { setTitle(it) }

                val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata.description.subtitle?.toString()
                artist?.let {setArtist(it)}

                val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
                    ?: metadata.description.description?.toString()
                album?.let {setAlbum(it)}

                metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)?.let {
                    if (it.endsWith(".lrc")) {
                        setLyric(it.toUri())
                    }
                }

                val lyricContent = metadata.getString("android.media.metadata.LYRICS")
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                lyricContent?.let {setLyricContent(it)}

                val lyricSentence = metadata.getString("android.media.metadata.CURRENT_LYRIC")
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                lyricSentence?.let {setLyricSentence(it)}

                setArtwork(handleArtwork(metadata))
                setLaunchIntent(getLaunchPendingIntent())
                setPlayerIntent(getPlayerPendingIntent())
            }
        }

        bridge.updateWithPlaybackInfo(playbackInfo)
    }

    fun cleanExpiredCache() {
        // 1. Clean internal cache
        cleanDirectory(context.cacheDir)

        // 2. Clean external cache (if available)
        context.externalCacheDir?.let { cleanDirectory(it) }

        // 3. Clean public directory (if used)
        val publicDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "voiceapp")
        if (publicDir.exists()) {
            cleanDirectory(publicDir)
        }
    }

    private fun cleanDirectory(directory: File) {
        val expirationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

        directory.listFiles()?.forEach { file ->
            when {
                // Handle artwork files
                file.isFile && shouldDeleteCacheFile(file, expirationTime) -> {
                    file.delete()
                }
                // Handle artwork subdirectories
                file.isDirectory && file.name == "artwork" -> {
                    cleanDirectory(file) // Recursively clean artwork subdirectory
                }
            }
        }
    }

    private fun shouldDeleteCacheFile(file: File, expirationTime: Long): Boolean {
        return file.name.startsWith("artwork_") &&
                file.name.endsWith(".jpg") &&
                file.lastModified() < expirationTime
    }
}