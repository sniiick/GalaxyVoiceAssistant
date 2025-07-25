package com.example.voiceapp3

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder.AudioSource
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.airbnb.lottie.LottieAnimationView
import com.example.voiceapp3.car.CarAudioPlayer
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.handlers.IntentHandlerRegistry
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.IOException

class VoiceAssistantService : Service() {
    companion object {
        private const val TAG = "VoiceAssistantService"
        private const val MAIN_CHANNEL_ID = "voice_assistant_channel"
        private const val PERMISSION_CHANNEL_ID = "permission_channel"
        private const val PERMISSION_NOTIFICATION_ID = 1001
        private const val VOICE_MODEL_DIR = "voice-model"
        private const val VOICE_MODEL_SOURCE = "model-ru"
        private const val CLASS_MODEL_SOURCE = "model-cmd"
        private const val ACTION_ECARX_KEY = "ecarx.intent.action.ECARX_KEY_RVOICEASSIST_EVENT"
        private const val RECOGNITION_TIMEOUT = 7500L
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    }
    private val handler = Handler(Looper.getMainLooper())
    private val commandHandler = Handler(Looper.getMainLooper())
    private var pendingCommand: Runnable? = null
    private val timeoutRunnable = Runnable {
        Log.i(TAG, "Recognition timeout reached")
        stopRecognition()
    }
    private lateinit var vehiclePropertyHelper: VehiclePropertyHelper
    private lateinit var carAudioPlayer: CarAudioPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var model: Model? = null
    private lateinit var intentModel: ActionModel
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recognitionThread: Thread? = null
    private var shouldContinueRecording = false
    private var isRecognizing = false
    private var isRecognized = false
    private var isPaused = false
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val commandSeparators = listOf(
        "и", "затем", "а затем", "потом", "а еще", "а потом",
        "после этого", "после того", "далее", "также"
    )

    private fun checkAndRequestPermissions(): Boolean {
        if (!PermissionHelper.hasAudioPermission(this)) {
            showPermissionNotification()
            return false
        }
        return true
    }

    private fun showPermissionNotification() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, PERMISSION_CHANNEL_ID)
            .setContentTitle("Microphone Access Required")
            .setContentText("Tap to enable microphone permissions")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
            PERMISSION_NOTIFICATION_ID,
            notification
        )
    }

    private val keyEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_ECARX_KEY) {
                Log.i(TAG, "ECARX key event received")
                toggleRecognition()
            }
        }
    }

    override fun onCreate() {
        System.setProperty("java.regex.implementation", "com.ibm.icu.impl.regex.RE2Impl")
        System.setProperty("java.util.regex.Pattern.impl", "com.ibm.icu.impl.regex.PatternImpl")

        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        Log.i(TAG, "Service creating")
        if (isBackgroundRestricted()) {
            Log.w(TAG, "Background restricted - guide user to disable")
        }
        checkAndRequestPermissions()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        vehiclePropertyHelper = VehiclePropertyHelper(applicationContext)
        carAudioPlayer = CarAudioPlayer(applicationContext)
        initMediaPlayer()
        initAudioFocusRequest()
        preloadSounds()
        registerKeyReceiver()
        initModels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service starting")
        startForegroundService()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerKeyReceiver() {
        val filter = IntentFilter(ACTION_ECARX_KEY).apply {
            this.priority = 999
            this.addCategory("android.intent.category.DEFAULT")
        }
        registerReceiver(keyEventReceiver, filter)
        Log.i(TAG, "Registered ECARX key receiver")
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                try {
                    mp.reset()
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting MediaPlayer", e)
                }
                true
            }
        }
    }
    private fun initAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        stopRecognition()
                        mediaPlayer.reset()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseRecognition()
                    AudioManager.AUDIOFOCUS_GAIN -> resumeRecognition()
                }
            }
            .build()
    }

    private fun initOverlay() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
            return
        }
        try {
            // Remove existing overlay if present
            windowManager?.removeView(overlayView)

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            windowManager?.addView(overlayView, params)
            overlayView?.visibility = View.VISIBLE
            val lottieView = overlayView?.findViewById<LottieAnimationView>(R.id.mic_animation)
            lottieView?.apply {
                setAnimation(R.raw.mic_pulse)
                playAnimation()
            }
            Log.i(TAG, "Overlay initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Overlay initialization failed", e)
        }
    }

    private fun updateStatusText(text: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                overlayView?.findViewById<TextView>(R.id.listening_status_text)?.text = text
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status text", e)
            }
        }
    }

    private fun initModels() {
        Log.i(TAG, "Initializing VOSK model")
        StorageService.unpack(this, VOICE_MODEL_SOURCE, VOICE_MODEL_DIR,
            { model: Model? ->
                this.model = model
                recognizer = Recognizer(model, 16000.0f)
                Log.i(TAG, "Voice model initialized successfully")
            },
            { exception: IOException? ->
                Log.e(TAG, "Failed to initialize model", exception)
            })
        Log.i(TAG, "Initializing Class model")
        intentModel = ActionModel(applicationContext, CLASS_MODEL_SOURCE)
        Log.i(TAG, "Class model initialized successfully")
    }

    private fun logOverlayState() {
        Log.i(TAG, "Overlay state - " +
                "View: ${overlayView != null}, " +
                "WindowManager: ${windowManager != null}, " +
                "Visible: ${overlayView?.visibility == View.VISIBLE}")
    }
    private fun toggleRecognition() {
        logOverlayState()
        if (model == null || recognizer == null) {
            Log.w(TAG, "Model not initialized, cannot start recognition")
            Toast.makeText(this, "Голосовая модель ещё не загружена", Toast.LENGTH_SHORT).show()
            return
        }

        if (isRecognizing) {
            stopRecognition()
        } else {
            startRecognition()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecognition() {
        if (!PermissionHelper.hasAudioPermission(this)) {
            Log.w(TAG, "Audio permission not granted")
            return
        }

        try {
            playSound(R.raw.overlay_start)
            requestAudioFocus()
            showOverlay()
            updateStatusText("...слушаю...")

            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord.Builder()
                .setAudioSource(AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build())
                .setBufferSizeInBytes(minBufferSize * 4)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                stopRecognition()
                return
            }

            // Initialize recognizer if not already done
            if (recognizer == null && model != null) {
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
            }

            shouldContinueRecording = true
            isRecognizing = true
            showOverlay()
            startTimeout()

            // Create recognition thread
            recognitionThread = Thread({
                val buffer = ByteArray(BUFFER_SIZE)
                audioRecord?.startRecording()

                while (shouldContinueRecording) {
                    if (!isPaused) {  // Only process audio when not paused
                        val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (bytesRead > 0) {
                            // Process audio data with VOSK
                            if (recognizer?.acceptWaveForm(buffer, bytesRead) == true) {
                                val result = recognizer?.result
                                handler.post {
                                    result?.let { processRecognitionResult(it) }
                                }
                            } else {
                                val partialResult = recognizer?.partialResult
                                handler.post {
                                    partialResult?.let { processPartialResult(it) }
                                }
                            }
                        }
                    } else {
                        // Sleep briefly when paused to avoid busy waiting
                        Thread.sleep(50)
                    }
                }
            }, "AudioRecognition Thread")

            recognitionThread?.start()
            Log.i(TAG, "Recognition started with custom AudioRecord")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition", e)
            stopRecognition()
        }
    }

    private fun startTimeout(timeout: Long = RECOGNITION_TIMEOUT) {
        handler.postDelayed(timeoutRunnable, timeout)
    }

    private fun resetTimeout() {
        handler.removeCallbacks(timeoutRunnable)
        Log.i(TAG, "Reset timeout for 5s")
        startTimeout(5000)
    }

    private fun stopRecognition() {
        if (!isRecognizing) return

        pendingCommand?.let { commandHandler.removeCallbacks(it) }
        pendingCommand = null
        handler.removeCallbacks(timeoutRunnable)
        updateStatusText("")
        hideOverlay()

        try {
            shouldContinueRecording = false
            isPaused = false  // Reset pause state when stopping
            recognitionThread?.join()
            recognitionThread = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            // Store the recognition success state before getting final result
            val wasRecognized = isRecognized

            // Get final result (this might modify isRecognized)
            val finalResult = recognizer?.finalResult
            finalResult?.let { processRecognitionResult(it) }

            // Use the stored value to determine which sound to play
            if (wasRecognized) {
                playSound(R.raw.success)
            } else {
                playSound(R.raw.overlay_stop)
            }
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        } finally {
            isRecognizing = false
            Log.i(TAG, "Recognition stopped")
        }
    }

    private fun pauseRecognition() {
        if (!isRecognizing || isPaused) return

        isPaused = true
        audioRecord?.stop()
        Log.i(TAG, "Recognition paused")
    }

    private fun resumeRecognition() {
        if (!isRecognizing || !isPaused) return

        try {
            audioRecord?.startRecording()
            isPaused = false
            Log.i(TAG, "Recognition resumed")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error resuming recording", e)
            stopRecognition()
        }
    }

    private fun requestAudioFocus(): Boolean {
        return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        Log.i(TAG, "Audio focus abandoned")
    }

    private fun preloadSounds() {
        listOf(R.raw.overlay_start, R.raw.overlay_stop, R.raw.success).forEach { resId ->
            try {
                val afd = resources.openRawResourceFd(resId)
                afd.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading sound $resId", e)
            }
        }
    }
    private fun playSound(resourceId: Int) {
        try {
            mediaPlayer.reset()

            val assetFileDescriptor = resources.openRawResourceFd(resourceId) ?: run {
                Log.e(TAG, "Resource not found: $resourceId")
                return
            }

            try {
                mediaPlayer.setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { mp ->
                    try {
                        mp.setVolume(0.8f, 0.8f)
                        mp.start()
                        Log.i(TAG, "Resource $resourceId played")
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error starting MediaPlayer", e)
                    }
                }

                mediaPlayer.setOnCompletionListener {
                    assetFileDescriptor.close()
                }

            } catch (e: Exception) {
                assetFileDescriptor.close()
                Log.e(TAG, "Error setting data source", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "General playSound error", e)
        }
    }

    private fun showOverlay() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            return
        }

        Handler(Looper.getMainLooper()).post {
            try {
                if (overlayView == null) {
                    initOverlay()
                }
                overlayView?.visibility = View.VISIBLE
                Log.i(TAG, "Overlay shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
                initOverlay()
                overlayView?.visibility = View.VISIBLE
            }
        }
    }

    private fun hideOverlay() {
        overlayView?.visibility = View.GONE
        Log.i(TAG, "Overlay hidden")
    }

    private fun splitMultipleCommands(text: String): List<String> {
        val pattern = Regex("""(?<=[^\s])\s+(?:${commandSeparators.joinToString("|")})\s+(?=[^\s])""")

        return text.split(pattern)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun processRecognitionResult(result: String) {
        try {
            val jsonObject = JSONObject(result)
            val text = jsonObject.optString("text", "")

            if (text.isNotEmpty()) {
                Log.i(TAG, "Result got: $text")
                updateStatusText(text)

                pendingCommand?.let { commandHandler.removeCallbacks(it) }

                if (processCommand(text)) {
                    isRecognized = true
                    stopRecognition()
                } else {
                    resetListeningStatus()
                    isRecognized = false
                    resetTimeout()
                }
            } else {
                resetListeningStatus()
                isRecognized = false
                Log.i(TAG, "Result is empty")
            }
        } catch (e: JSONException) {
            resetListeningStatus()
            isRecognized = false
            resetTimeout()
            Log.e(TAG, "Error parsing result JSON", e)
        }
    }

    private fun processPartialResult(partialResult: String) {
        try {
            val jsonObject = JSONObject(partialResult)
            val partialText = jsonObject.optString("partial", "")

            if (partialText.isNotEmpty()) {
                Log.i(TAG, "Partial result (valid speech): $partialText")
                updateStatusText(partialText)

                pendingCommand?.let { commandHandler.removeCallbacks(it) }
                pendingCommand = Runnable {
                    if (processCommand(partialText)) {
                        isRecognized = true
                        stopRecognition()
                    } else {
                        isRecognized = false
                        resetTimeout()
                    }
                }
                commandHandler.postDelayed(pendingCommand!!, 300)
            } else {
                isRecognized = false
                Log.i(TAG, "Partial result (empty JSON, ignoring)")
            }
        } catch (e: JSONException) {
            isRecognized = false
            resetTimeout()
            Log.e(TAG, "Error parsing partial result JSON", e)
        }
    }

    private fun processCommand(text: String?) : Boolean {
        if (text.isNullOrEmpty()) {
            Log.w(TAG, "Received null or empty command")
            return false
        }

        val commands = splitMultipleCommands(text)

        if (commands.size > 1) {
            Log.i(TAG, "Detected multiple commands: ${commands.size}")
            return processCommandSequence(commands)
        }

        return processSingleCommand(text)
    }

    private fun processCommandSequence(commands: List<String>): Boolean {
        var anySuccess = false
        commands.forEachIndexed { index, command ->
            Log.i(TAG, "Processing command ${index + 1}/${commands.size}: '$command'")
            val success = processSingleCommand(command)

            if (success) {
                anySuccess = true
            } else {
                Log.w(TAG, "Command $index failed: '$command'")
            }
        }
        return anySuccess
    }

    private fun processSingleCommand(text: String): Boolean {
        val prediction = intentModel.predict(text)

        if (prediction.confidence.times(100).toInt() < 40) {
            Log.i(TAG, "Confidence too low: ${prediction.confidence}")
            return false
        }

        Log.i(TAG, """
        Command: ${prediction.text}
        Normalized: ${prediction.normalizedText}
        Intent: ${prediction.intent} (${(prediction.confidence.times(100)).toInt()}% confidence)
        Entities: ${prediction.entities}
        """.trimIndent())

        return IntentHandlerRegistry(vehiclePropertyHelper, carAudioPlayer, applicationContext).handle(prediction)
    }
    private fun resetListeningStatus() {
        handler.postDelayed({
            updateStatusText("...слушаю...")
        }, 500) // 0.5s delay
    }
    private fun cleanup() {
        try {
            unregisterReceiver(keyEventReceiver)

            // Stop and release audio resources
            shouldContinueRecording = false
            isRecognizing = false
            isPaused = false

            // Stop and cleanup recognition thread
            recognitionThread?.interrupt()
            recognitionThread?.join()
            recognitionThread = null

            // Release AudioRecord
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            } catch (e: IllegalStateException) {
                Log.w(TAG, "AudioRecord already released", e)
            }

            // Cleanup VOSK resources
            recognizer?.reset()
            model?.close()
            intentModel.cleanup()

            // Release media resources
            mediaPlayer.release()
            carAudioPlayer.release()

            // Cleanup vehicle connection
            vehiclePropertyHelper.disconnect()

            // Remove overlay
            hideOverlay()
            windowManager?.removeView(overlayView)
            overlayView = null
            windowManager = null

            Log.i(TAG, "All resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun isBackgroundRestricted(): Boolean {
        return (getSystemService(ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        val mainChannel = NotificationChannel(
            MAIN_CHANNEL_ID,
            "Voice Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Voice recognition service notifications"
        }

        val permissionChannel = NotificationChannel(
            PERMISSION_CHANNEL_ID,
            "Permission Requests",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important permission requests"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(mainChannel)
        notificationManager.createNotificationChannel(permissionChannel)
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
