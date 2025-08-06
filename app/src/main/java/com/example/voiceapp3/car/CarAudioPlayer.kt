package com.example.voiceapp3.car

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.voiceapp3.tools.CarModel


class CarAudioPlayer(private val context: Context) {
    private val TAG: String = "CarAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null



    @SuppressLint("WrongConstant")
    fun playWithCustomUsage(assetName: String) {
        try {
            val externalUsage = CarModel.getExternalUsage()

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val attributes = AudioAttributes.Builder()
                    .setUsage(externalUsage)
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

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}