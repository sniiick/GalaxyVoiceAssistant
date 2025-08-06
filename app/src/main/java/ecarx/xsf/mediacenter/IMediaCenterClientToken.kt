package ecarx.xsf.mediacenter

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

interface IMediaCenterClientToken : IInterface {
    abstract class Stub : Binder(), IMediaCenterClientToken {
        override fun asBinder(): IBinder = this

        private class Proxy(private val remote: IBinder) : IMediaCenterClientToken {
            override fun asBinder(): IBinder = remote
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        companion object {
            const val DESCRIPTOR = "ecarx.xsf.mediacenter.IMediaCenterClientToken"

            fun asInterface(binder: IBinder?): IMediaCenterClientToken? {
                if (binder == null) return null
                val iin = binder.queryLocalInterface(DESCRIPTOR)
                return if (iin != null && iin is IMediaCenterClientToken) {
                    iin
                } else {
                    Proxy(binder)
                }
            }
        }
    }
}