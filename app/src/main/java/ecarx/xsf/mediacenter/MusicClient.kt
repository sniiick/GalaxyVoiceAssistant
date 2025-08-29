package ecarx.xsf.mediacenter

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import android.view.KeyEvent


class MusicClient(private val context: Context) : IMusicClient {

    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private var lastEventTime = 0L
    private var currentKeyCode = 0

    // TODO("need to pass playback info?")
    private val info = MusicPlaybackInfo()

    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            Log.d("LiveMusicClient", "onTransact CALLED: $code, $data, $reply, $flags")
            when (code) {
                INTERFACE_TRANSACTION -> {
                    reply?.writeString("ecarx.xsf.mediacenter.IMusicClient")
                    return true
                }
                TRANSACTION_getMusicPlaybackInfo -> {
                    data.enforceInterface(DESCRIPTOR)
                    val playbackInfo = getPlaybackInfo()
                    reply?.writeNoException()
                    reply?.writeStrongBinder(playbackInfo.asBinder())
                    return true
                }
                TRANSACTION_onNext -> {
                    data.enforceInterface(DESCRIPTOR)
                    sendMediaKey(KEYCODE_MEDIA_NEXT, true)
                    sendMediaKey(KEYCODE_MEDIA_NEXT, false)
                    reply?.writeNoException()
                    reply?.writeInt(1)
                    return true
                }
                TRANSACTION_onPrevious, TRANSACTION_onReplay -> {
                    data.enforceInterface(DESCRIPTOR)
                    sendMediaKey(KEYCODE_MEDIA_PREVIOUS, true)
                    sendMediaKey(KEYCODE_MEDIA_PREVIOUS, false)
                    reply?.writeNoException()
                    reply?.writeInt(1)
                    return true
                }
                TRANSACTION_onPlay, TRANSACTION_onPause -> {
                    data.enforceInterface(DESCRIPTOR)
                    sendMediaKey(KEYCODE_MEDIA_CONFIRM, true)
                    sendMediaKey(KEYCODE_MEDIA_CONFIRM, false)
                    reply?.writeNoException()
                    reply?.writeInt(1)
                    return true
                }
                TRANSACTION_onForward -> {
                    data.enforceInterface(DESCRIPTOR)
                    handleForwardRewind(KEYCODE_MEDIA_FORWARD)
                    reply?.writeNoException()
                    reply?.writeInt(1)
                    return true
                }
                TRANSACTION_onRewind -> {
                    data.enforceInterface(DESCRIPTOR)
                    handleForwardRewind(KEYCODE_MEDIA_REWIND)
                    reply?.writeNoException()
                    reply?.writeInt(1)
                    return true
                }
                else -> return super.onTransact(code, data, reply, flags)
            }
        }
    }

    private fun startKeyRepeat(keyCode: Int) {
        currentKeyCode = keyCode
        lastEventTime = System.currentTimeMillis()

        if (repeatRunnable == null) {
            repeatRunnable = object : Runnable {
                override fun run() {
                    if (System.currentTimeMillis() - lastEventTime > INACTIVITY_TIMEOUT) {
                        stopKeyRepeat()
                        return
                    }

                    sendMediaKey(currentKeyCode, true)
                    handler.postDelayed(this, REPEAT_DELAY_MS)
                }
            }
            handler.post(repeatRunnable!!)
        } else {
            lastEventTime = System.currentTimeMillis()
        }
    }

    private fun stopKeyRepeat() {
        repeatRunnable?.let {
            handler.removeCallbacks(it)
            repeatRunnable = null
        }
        sendMediaKey(currentKeyCode, false)
    }

    private fun handleForwardRewind(keyCode: Int) {
        when (keyCode) {
            KEYCODE_MEDIA_FORWARD, KEYCODE_MEDIA_REWIND -> startKeyRepeat(keyCode)
        }
    }

    fun sendMediaKey(keyCode: Int, press: Boolean) {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val action = if (press) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(action, keyCode))
            Log.i("LiveMusicClient", "Sent ${if (press) "press" else "release"} for key $keyCode")
        } catch (e: Exception) {
            Log.e("LiveMusicClient", "Error sending key $keyCode: $e")
        }
    }

    override fun asBinder(): IBinder = binder

    override fun getPlaybackInfo(): IMusicPlaybackInfo {
        Log.d("iMusicClient", "getPlaybackInfo CALLED!")
        return info
    }

    companion object {
        const val DESCRIPTOR = "ecarx.xsf.mediacenter.IMusicClient"
        const val TRANSACTION_onPlay = 1
        const val TRANSACTION_onPause = 2
        const val TRANSACTION_onNext = 3
        const val TRANSACTION_onPrevious = 4
        const val TRANSACTION_onForward = 5
        const val TRANSACTION_onRewind = 6
        const val TRANSACTION_onSourceSelected = 8
        const val TRANSACTION_onMediaSelected = 9
        const val TRANSACTION_getMusicPlaybackInfo = 10
        const val TRANSACTION_getCurrentSourceType = 12
        const val TRANSACTION_getCurrentProgress = 13
        const val TRANSACTION_onSourceChanged = 17
        const val TRANSACTION_onReplay = 18
        const val TRANSACTION_onMediaSelectedPlay = 21
        const val TRANSACTION_onMediaForward = 22
        const val TRANSACTION_onMediaCenterFocusChanged = 25
        const val TRANSACTION_onExit = 26
        const val TRANSACTION_operationType = 33

        const val KEYCODE_MEDIA_PREVIOUS = KeyEvent.KEYCODE_MEDIA_PREVIOUS
        const val KEYCODE_MEDIA_NEXT = KeyEvent.KEYCODE_MEDIA_NEXT
        const val KEYCODE_MEDIA_CONFIRM = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        const val KEYCODE_MEDIA_FORWARD = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
        const val KEYCODE_MEDIA_REWIND = KeyEvent.KEYCODE_MEDIA_REWIND
        const val REPEAT_DELAY_MS = 100L // Delay between repeated key presses
        const val INACTIVITY_TIMEOUT = 600L // Stop after 600ms of no events
    }
}


interface IMusicClient {
    fun getPlaybackInfo(): IMusicPlaybackInfo
    fun asBinder(): IBinder
}
