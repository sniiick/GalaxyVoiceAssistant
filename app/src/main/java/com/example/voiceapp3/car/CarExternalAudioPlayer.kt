package com.example.voiceapp3.car

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class CarAudioPlayer(private val context: Context) {
    private val TAG: String = "CarAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    fun playWithCustomUsage(assetName: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val usage = getCustomUsageConstant()
                val attributes = AudioAttributes.Builder()
                    .setUsage(usage)
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
            Log.i(TAG, "Error in playing sound: ${e.toString()}")
        }
    }

    private fun getCustomUsageConstant(): Int {
        return try {
            val field = AudioAttributes::class.java.getDeclaredField("USAGE_EXT_SOUND")
            field.getInt(null)
        } catch (e: Exception) {
            Log.i(TAG, "Error in getting custom usageName: ${e.toString()}")
            AudioAttributes.USAGE_MEDIA
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}