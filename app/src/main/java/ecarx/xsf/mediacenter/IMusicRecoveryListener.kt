package ecarx.xsf.mediacenter

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

interface IMusicRecoveryListener : IInterface {
    fun onGetMediaList(info: IRecoveryMediaMetaInfo?)
    fun onGetMusicPlaybackInfo(info: IMusicPlaybackInfo?)

    abstract class Stub : Binder(), IMusicRecoveryListener {
        override fun asBinder(): IBinder = this

        private class Proxy(private val mRemote: IBinder) : IMusicRecoveryListener {
            override fun asBinder(): IBinder = mRemote

            override fun onGetMediaList(info: IRecoveryMediaMetaInfo?) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    if (info != null) {
                        data.writeInt(1)
                        info.writeToParcel(data, 0)
                    } else {
                        data.writeInt(0)
                    }
                    mRemote.transact(TRANSACTION_onGetMediaList, data, reply, 0)
                    reply.readException()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            override fun onGetMusicPlaybackInfo(info: IMusicPlaybackInfo?) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(info?.asBinder())
                    mRemote.transact(TRANSACTION_onGetMusicPlaybackInfo, data, reply, 0)
                    reply.readException()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    true
                }
                TRANSACTION_onGetMediaList -> {
                    data.enforceInterface(DESCRIPTOR)
                    val info = if (data.readInt() != 0) {
                        IRecoveryMediaMetaInfo.CREATOR.createFromParcel(data)
                    } else {
                        null
                    }
                    onGetMediaList(info)
                    reply?.writeNoException()
                    true
                }
                TRANSACTION_onGetMusicPlaybackInfo -> {
                    data.enforceInterface(DESCRIPTOR)
                    val info = IMusicPlaybackInfo.Stub.asInterface(data.readStrongBinder())
                    onGetMusicPlaybackInfo(info)
                    reply?.writeNoException()
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        companion object {
            const val DESCRIPTOR = "ecarx.xsf.mediacenter.staterecover.IMusicRecoveryListener"
            const val TRANSACTION_onGetMediaList = 1
            const val TRANSACTION_onGetMusicPlaybackInfo = 2

            fun asInterface(binder: IBinder?): IMusicRecoveryListener? {
                if (binder == null) return null
                val iin = binder.queryLocalInterface(DESCRIPTOR)
                return if (iin != null && iin is IMusicRecoveryListener) {
                    iin
                } else {
                    Proxy(binder)
                }
            }
        }
    }
}