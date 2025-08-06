package ecarx.xsf.mediacenter

import android.app.PendingIntent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import java.util.UUID

class MusicPlaybackInfo() : IMusicPlaybackInfo.Stub() {
    var packageName: String = "ru.yandex.music"
    var appName: String = "YandexMusic"
    var title: String = ""
    var artist: String = ""
    var album: String = ""
    var lyric: Uri = Uri.EMPTY
    var lyricContent: String = ""
    var lyricSentence: String = ""
    var artwork: Uri = Uri.EMPTY
    var duration: Long = 0
    var radioMode: Int = 0
    var radioStationName: String = ""
    var playbackStatus: Int = 1 // 2=stopped, 0=paused, 1=playing
    var sourceType: Int = 6
    var launchIntent: PendingIntent? = null
    var playerIntent: PendingIntent? = null
    var iconUri: String = ""

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        when (code) {
            INTERFACE_TRANSACTION -> {
                reply?.writeString("ecarx.xsf.mediacenter.IMusicPlaybackInfo")
                return true
            }
            TRANSACTION_getTitle -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(title)
                return true
            }
            TRANSACTION_getArtist -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(artist)
                return true
            }
            TRANSACTION_getAlbum -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(album)
                return true
            }
            TRANSACTION_getDuration -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeLong(duration)
                return true
            }
            TRANSACTION_getPlaybackStatus -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeInt(playbackStatus)
                return true
            }
            TRANSACTION_getSourceType -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeInt(sourceType)
                return true
            }
            TRANSACTION_getPackageName -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(packageName)
                return true
            }
            TRANSACTION_getAppName -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(appName)
                return true
            }
            TRANSACTION_getUuid -> {
                data.enforceInterface(DESCRIPTOR)
                val uuid = UUID.randomUUID().toString()
                reply?.writeNoException()
                reply?.writeString(uuid)
                return true
            }
            TRANSACTION_getAppIcon -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(iconUri)
                return true
            }
            TRANSACTION_getRadioStationName -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(radioStationName)
                return true
            }
            TRANSACTION_getLaunchIntent -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                if (launchIntent != null) {
                    reply?.writeInt(1)
                    launchIntent!!.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
                } else {
                    reply?.writeInt(0) // failure
                    reply?.writeStrongBinder(null)
                }
                return true
            }
            TRANSACTION_getPlayerIntent -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                if (playerIntent != null) {
                    reply?.writeInt(1)
                    playerIntent!!.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
                } else {
                    reply?.writeInt(0) // failure
                    reply?.writeStrongBinder(null)
                }
                return true
            }
            TRANSACTION_getRadioMode -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeInt(radioMode)
                return true
            }
            TRANSACTION_getArtwork -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeInt(1)
                artwork.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
                return true
            }
            TRANSACTION_getLyric -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeInt(1)
                lyric.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
                return true
            }
            TRANSACTION_getLyricContent -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(lyricContent)
                return true
            }
            TRANSACTION_getCurrentLyricSentence -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeString(lyricSentence)
                return true
            }
            5, 8, 10, 15, 17, in 18..23, 28, 29, 30, 31, 32 -> {
                return super.onTransact(code, data, reply, flags)
            }
            else -> {
                Log.i("LiveMusicPlaybackInfo", "onTransact NOT HANDLED CALLED: $code, $data, $reply, $flags")
                return super.onTransact(code, data, reply, flags)
            }
        }
    }

    companion object {
        const val DESCRIPTOR = "ecarx.xsf.mediacenter.IMusicPlaybackInfo"
        const val TRANSACTION_getAlbum = 4
        const val TRANSACTION_getAppIcon = 26
        const val TRANSACTION_getAppName = 25
        const val TRANSACTION_getArtist = 3
        const val TRANSACTION_getArtwork = 16
        const val TRANSACTION_getLyricContent = 12
        const val TRANSACTION_getLyric = 13
        const val TRANSACTION_getCurrentLyricSentence = 14
        const val TRANSACTION_getLaunchIntent = 1
        const val TRANSACTION_getDuration = 7
        const val TRANSACTION_getRadioMode = 19
        const val TRANSACTION_getRadioStationName = 6
        const val TRANSACTION_getPlayerIntent = 33
        const val TRANSACTION_getPackageName = 27
        const val TRANSACTION_getPlaybackStatus = 11
        const val TRANSACTION_getSourceType = 9
        const val TRANSACTION_getTitle = 2
        const val TRANSACTION_getUuid = 24
    }
}


interface IMusicPlaybackInfo : IInterface {
    abstract class Stub : Binder(), IMusicPlaybackInfo {
        override fun asBinder(): IBinder = this

        companion object {
            const val DESCRIPTOR = "ecarx.xsf.mediacenter.IMusicPlaybackInfo"

            fun asInterface(binder: IBinder?): IMusicPlaybackInfo? {
                if (binder == null) return null
                val iin = binder.queryLocalInterface(DESCRIPTOR)
                return if (iin != null && iin is IMusicPlaybackInfo) {
                    iin
                } else {
                    object : IMusicPlaybackInfo {
                        override fun asBinder(): IBinder = binder
                    }
                }
            }
        }
    }
}