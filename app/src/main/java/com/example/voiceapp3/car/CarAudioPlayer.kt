package com.example.voiceapp3.car

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.ModelEnum
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class CarAudioPlayer(private val context: Context) {
    private val TAG: String = "CarAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var modelStorageDir: File = File(context.filesDir, "tts_models")
    private lateinit var tts: OfflineTts

    companion object {
        private const val CAR_EXTERNAL_USAGE_STARSHIP = 29
        private const val CAR_EXTERNAL_USAGE_E5 = 73
        private const val DEFAULT_USAGE = 1
        private const val ASSETS_MODEL_DIR = "tts-ru"
    }

    fun initialize() {
        try {
            copyModelAssets()
            val config = getOfflineTtsConfig()
            tts = OfflineTts(config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init TTS", e)
        }
    }

    fun getExternalUsage(): Int {
        return when (CarModel.getCarModel()) {
            ModelEnum.E5, ModelEnum.E5v2, ModelEnum.EX5 -> CAR_EXTERNAL_USAGE_E5
            ModelEnum.STARSHIP -> CAR_EXTERNAL_USAGE_STARSHIP
            ModelEnum.COOLRAY, ModelEnum.UNKNOWN -> DEFAULT_USAGE
        }
    }

    private fun copyModelAssets() {
        if (!modelStorageDir.exists()) {
            modelStorageDir.mkdirs()
        }

        try {
            copyAssetsRecursively(ASSETS_MODEL_DIR, modelStorageDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying model assets: ${e.message}")
        }
    }

    private fun copyAssetsRecursively(assetPath: String, destinationDir: File) {
        try {
            val assets = context.assets.list(assetPath)
            assets?.forEach { assetName ->
                val fullAssetPath = if (assetPath.isEmpty()) assetName else "$assetPath/$assetName"
                val destFile = File(destinationDir, assetName)

                // Skip if destination already exists and is not empty
                if (destFile.exists()) {
                    if (destFile.isDirectory && destFile.list()?.isNotEmpty() == true) {
                        Log.d(TAG, "Skipping directory (already exists): $assetName")
                        return@forEach
                    } else if (destFile.isFile && destFile.length() > 0) {
                        Log.d(TAG, "Skipping file (already exists): $assetName")
                        return@forEach
                    } else {
                        // Empty file or directory, delete and recopy
                        destFile.deleteRecursively()
                    }
                }

                try {
                    // Try to open as file first - this is more reliable
                    try {
                        context.assets.open(fullAssetPath).use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        Log.d(TAG, "Copied file: $assetName (${destFile.length()} bytes)")
                    } catch (e: FileNotFoundException) {
                        // If opening as file fails, it's probably a directory
                        destFile.mkdirs()
                        copyAssetsRecursively(fullAssetPath, destFile)
                        Log.d(TAG, "Created directory: $assetName")
                    } catch (e: IOException) {
                        // Handle other IO exceptions
                        Log.e(TAG, "Error copying file $assetName: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing $assetName: ${e.message}")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error listing assets in $assetPath: ${e.message}")
        }
    }

    fun getOfflineTtsConfig(): OfflineTtsConfig {
        val vits = OfflineTtsVitsModelConfig.Builder()
            .setModel(File(modelStorageDir, "ru_RU-dmitri-medium.onnx").absolutePath)
            .setTokens(File(modelStorageDir, "tokens.txt").absolutePath)
            .setDataDir(File(modelStorageDir, "espeak-ng-data").absolutePath)
            .build()

        return OfflineTtsConfig.Builder()
            .setModel(
                OfflineTtsModelConfig.Builder()
                    .setVits(vits)
                    .setNumThreads(2)
                    .setDebug(true)
                    .setProvider("cpu")
                    .build()
            )
            .build()
    }

    fun playWithCustomUsage(assetName: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val attributes = AudioAttributes.Builder()
                    .setUsage(getExternalUsage())
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(attributes)

                val assetFileDescriptor = context.assets.openFd("media/$assetName")
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                assetFileDescriptor.close()

                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in playing sound: ${e.toString()}")
        }
    }

    fun playText(text: String) {
        try {
            val audio = tts.generate(text)
            if (audio != null && audio.samples.isNotEmpty()) {
                playGeneratedAudio(audio.samples, audio.sampleRate)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS generation: ${e.message}")
        }
    }

    private fun playGeneratedAudio(samples: FloatArray, sampleRate: Int) {
        try {
            stopAudioTrack()

            val pcmData = convertFloatToPCM16(samples)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(getExternalUsage())
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.apply {
                setVolume(AudioTrack.getMaxVolume())
                write(pcmData, 0, pcmData.size, AudioTrack.WRITE_BLOCKING)

                setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack) {
                        stopAudioTrack()
                    }

                    override fun onPeriodicNotification(track: AudioTrack) {
                    }
                })

                notificationMarkerPosition = pcmData.size / 2 // Divide by 2 because 16-bit = 2 bytes per sample

                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing generated audio: ${e.message}")
        }
    }

    private fun convertFloatToPCM16(floatSamples: FloatArray): ByteArray {
        val pcmData = ByteArray(floatSamples.size * 2)
        for (i in floatSamples.indices) {
            val sample = (floatSamples[i].coerceIn(-1.0f, 1.0f) * 32767).toInt()
            pcmData[i * 2] = (sample and 0xFF).toByte()
            pcmData[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return pcmData
    }

    private fun stopAudioTrack() {
        audioTrack?.apply {
            if (playState != AudioTrack.PLAYSTATE_STOPPED) {
                stop()
            }
            release()
        }
        audioTrack = null
    }

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopAudioTrack()
    }
}
