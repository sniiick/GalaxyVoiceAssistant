package ecarx.xsf.mediacenter

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

interface IStateRecoverApiSvc : IInterface {
    fun getRecoveryMediaList(token: IMediaCenterClientToken): IRecoveryMediaMetaInfo?
    fun getRecoveryMusicPlaybackInfo(token: IMediaCenterClientToken): IMusicPlaybackInfo?
    fun onMusicRecoveryComplete(token: IMediaCenterClientToken)
    fun registerMusicRecoveryIntent(token: IMediaCenterClientToken, flags: Int, pkg: String): Boolean
    fun setMusicRecoveryCallback(token: IMediaCenterClientToken, listener: IMusicRecoveryListener): Boolean
    fun unRegisterMusicRecoveryIntent(token: IMediaCenterClientToken): Boolean

    abstract class Stub : Binder(), IStateRecoverApiSvc {
        override fun asBinder(): IBinder = this

        private class Proxy(private val mRemote: IBinder) : IStateRecoverApiSvc {
            override fun asBinder(): IBinder = mRemote

            override fun getRecoveryMediaList(token: IMediaCenterClientToken): IRecoveryMediaMetaInfo? {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_getRecoveryMediaList, data, reply, 0)
                    reply.readException()
                    if (reply.readInt() != 0) IRecoveryMediaMetaInfo.CREATOR.createFromParcel(reply) else null
                } finally { data.recycle(); reply.recycle() }
            }

            override fun getRecoveryMusicPlaybackInfo(token: IMediaCenterClientToken): IMusicPlaybackInfo? {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_getRecoveryMusicPlaybackInfo, data, reply, 0)
                    reply.readException()
                    IMusicPlaybackInfo.Stub.asInterface(reply.readStrongBinder())
                } finally { data.recycle(); reply.recycle() }
            }

            override fun onMusicRecoveryComplete(token: IMediaCenterClientToken) {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_onMusicRecoveryComplete, data, reply, 0)
                    reply.readException()
                } finally { data.recycle(); reply.recycle() }
            }

            override fun registerMusicRecoveryIntent(
                token: IMediaCenterClientToken,
                flags: Int,
                pkg: String
            ): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    data.writeInt(flags)
                    data.writeString(pkg)
                    mRemote.transact(TRANSACTION_registerMusicRecoveryIntent, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally { data.recycle(); reply.recycle() }
            }

            override fun setMusicRecoveryCallback(
                token: IMediaCenterClientToken,
                listener: IMusicRecoveryListener
            ): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    data.writeStrongBinder(listener.asBinder())
                    mRemote.transact(TRANSACTION_setMusicRecoveryCallback, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally { data.recycle(); reply.recycle() }
            }

            override fun unRegisterMusicRecoveryIntent(token: IMediaCenterClientToken): Boolean {
                val data = Parcel.obtain(); val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(token.asBinder())
                    mRemote.transact(TRANSACTION_unRegisterMusicRecoveryIntent, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally { data.recycle(); reply.recycle() }
            }
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    true
                }
                TRANSACTION_registerMusicRecoveryIntent -> {
                    data.enforceInterface(DESCRIPTOR)
                    val token = IMediaCenterClientToken.Stub.asInterface(data.readStrongBinder())
                    val flags = data.readInt()
                    val pkg = data.readString()
                    val result = registerMusicRecoveryIntent(token!!, flags, pkg!!)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    true
                }
                TRANSACTION_unRegisterMusicRecoveryIntent -> {
                    data.enforceInterface(DESCRIPTOR)
                    val token = IMediaCenterClientToken.Stub.asInterface(data.readStrongBinder())
                    val result = unRegisterMusicRecoveryIntent(token!!)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    true
                }
                TRANSACTION_getRecoveryMediaList -> {
                    data.enforceInterface(DESCRIPTOR)
                    val token = IMediaCenterClientToken.Stub.asInterface(data.readStrongBinder())
                    val result = getRecoveryMediaList(token!!)
                    reply?.writeNoException()
                    if (result != null) {
                        reply?.writeInt(1)
                        result.writeToParcel(reply!!, 0)
                    } else {
                        reply?.writeInt(0)
                    }
                    true
                }
                TRANSACTION_getRecoveryMusicPlaybackInfo -> {
                    data.enforceInterface(DESCRIPTOR)
                    val token = IMediaCenterClientToken.Stub.asInterface(data.readStrongBinder())
                    val result = getRecoveryMusicPlaybackInfo(token!!)
                    reply?.writeNoException()
                    reply?.writeStrongBinder(result?.asBinder())
                    true
                }
                TRANSACTION_setMusicRecoveryCallback -> {
                    data.enforceInterface(DESCRIPTOR)
                    val token = IMediaCenterClientToken.Stub.asInterface(data.readStrongBinder())
                    val listener = IMusicRecoveryListener.Stub.asInterface(data.readStrongBinder())
                    val result = setMusicRecoveryCallback(token!!, listener!!)
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    true
                }
                TRANSACTION_onMusicRecoveryComplete -> {
                    data.enforceInterface(DESCRIPTOR)
                    val token = IMediaCenterClientToken.Stub.asInterface(data.readStrongBinder())
                    onMusicRecoveryComplete(token!!)
                    reply?.writeNoException()
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        companion object {
            const val DESCRIPTOR = "ecarx.xsf.mediacenter.staterecover.IStateRecoverApiSvc"

            const val TRANSACTION_registerMusicRecoveryIntent = 1
            const val TRANSACTION_unRegisterMusicRecoveryIntent = 2
            const val TRANSACTION_getRecoveryMediaList = 3
            const val TRANSACTION_getRecoveryMusicPlaybackInfo = 4
            const val TRANSACTION_setMusicRecoveryCallback = 5
            const val TRANSACTION_onMusicRecoveryComplete = 6
        }
    }
}
