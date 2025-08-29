package ecarx.xsf.mediacenter

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

interface IMediaCenterSvc : IInterface {
    fun getStateBinder(): IBinder
    fun getMediaControlClientApi(): IBinder
    fun getMediaControllerApi(): IBinder
    fun queryCurrentFocusClient(token: IMediaCenterClientToken): String?

    fun registerMusic(client: IMusicClient): IMediaCenterClientToken?
    fun registerInMusic(pkg: String, client: IMusicClient): IMediaCenterClientToken?
    fun unregister(token: IMediaCenterClientToken): Boolean

    fun requestPlay(token: IMediaCenterClientToken): Boolean

    fun updateCurrentProgress(token: IMediaCenterClientToken, progress: Long): Boolean
    fun updateCurrentLyric(token: IMediaCenterClientToken, lyric: String): Boolean
    fun updateCurrentSourceType(token: IMediaCenterClientToken, sourceType: Int): Boolean
    fun updateMusicPlaybackState(token: IMediaCenterClientToken, info: IMusicPlaybackInfo): Boolean

    abstract class Stub : Binder(), IMediaCenterSvc {
        override fun asBinder(): IBinder = this

        private class Proxy(private val mRemote: IBinder) : IMediaCenterSvc {
            override fun asBinder(): IBinder = mRemote

            override fun getStateBinder(): IBinder {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_getStateBinder, data, reply, 0)
                    reply.readException()
                    reply.readStrongBinder()
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun getMediaControlClientApi(): IBinder {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_getMediaControlClientApi, data, reply, 0)
                    reply.readException()
                    reply.readStrongBinder()
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun getMediaControllerApi(): IBinder {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_getMediaControllerApi, data, reply, 0)
                    reply.readException()
                    reply.readStrongBinder()
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun registerInMusic(pkg: String, client: IMusicClient): IMediaCenterClientToken? {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(pkg)
                    data.writeStrongBinder(client.asBinder())
                    mRemote.transact(TRANSACTION_registerInMusic, data, reply, 0)
                    reply.readException()
                    val t = reply.readStrongBinder()
                    if (t != null) object : IMediaCenterClientToken { override fun asBinder() = t } else null
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun updateMusicPlaybackState(token: IMediaCenterClientToken, info: IMusicPlaybackInfo): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    data.writeStrongBinder(info.asBinder())
                    mRemote.transact(TRANSACTION_updateMusicPlaybackState, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun requestPlay(token: IMediaCenterClientToken): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_requestPlay, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun updateCurrentSourceType(token: IMediaCenterClientToken, sourceType: Int): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    data.writeInt(sourceType)
                    mRemote.transact(TRANSACTION_updateCurrentSourceType, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun updateCurrentLyric(token: IMediaCenterClientToken, lyric: String): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    data.writeString(lyric)
                    mRemote.transact(TRANSACTION_updateCurrentLyric, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun updateCurrentProgress(token: IMediaCenterClientToken, progress: Long): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    data.writeLong(progress)
                    mRemote.transact(TRANSACTION_updateCurrentProgress, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun registerMusic(client: IMusicClient): IMediaCenterClientToken? {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(client.asBinder())
                    mRemote.transact(TRANSACTION_registerMusic, data, reply, 0)
                    reply.readException()
                    val t = reply.readStrongBinder()
                    if (t != null) object : IMediaCenterClientToken { override fun asBinder() = t } else null
                } finally {
                    data.recycle(); reply.recycle()
                }
            }
            override fun queryCurrentFocusClient(token: IMediaCenterClientToken): String? {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_queryCurrentFocusClient, data, reply, 0)
                    reply.readException()
                    reply.readString()
                } finally {
                    data.recycle(); reply.recycle()
                }
            }

            override fun unregister(token: IMediaCenterClientToken): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_unregister, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    data.recycle(); reply.recycle()
                }
            }
        }

        companion object {
            const val DESCRIPTOR = "ecarx.xsf.mediacenter.IMediaCenterSvc"

            const val TRANSACTION_registerMusic = 1
            const val TRANSACTION_unregister = 4
            const val TRANSACTION_requestPlay = 5
            const val TRANSACTION_updateMusicPlaybackState = 6
            const val TRANSACTION_updateCurrentSourceType = 8
            const val TRANSACTION_updateCurrentProgress = 10
            const val TRANSACTION_updateCurrentLyric = 14
            const val TRANSACTION_registerInMusic = 19
            const val TRANSACTION_getStateBinder = 31
            const val TRANSACTION_getMediaControlClientApi = 32
            const val TRANSACTION_getMediaControllerApi = 33
            const val TRANSACTION_queryCurrentFocusClient = 42

            fun asInterface(b: IBinder?): IMediaCenterSvc? =
                b?.let { Proxy(it) }
        }
    }
}