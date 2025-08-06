package com.example.voiceapp3.car

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.example.voiceapp3.VoiceAssistantService
import com.example.voiceapp3.media.MediaSessionCoordinator
import ecarx.xsf.mediacenter.IMediaCenterClientToken
import ecarx.xsf.mediacenter.IMediaCenterSvc
import ecarx.xsf.mediacenter.MusicClient
import ecarx.xsf.mediacenter.MusicPlaybackInfo


class MediaCenterBridge(val context: Context, val service: VoiceAssistantService)  {
    companion object {
        private const val TAG = "McBridge"
        private const val SERVICE_ACTION = "ecarx.xsf.MEDIA_CENTER_SERVICE"
        private const val SERVICE_PKG  = "ecarx.xsf.mediacenter"
        private const val SERVICE_CLS  = "ecarx.xsf.mediacenter.MediaCenterService"
        private var isBound = false
        private var isConnecting = false
        private var lastConnectionTime: Long = 0
        private const val CONNECTION_TIMEOUT_MS = 3000L
    }

    private var svc: IMediaCenterSvc? = null
    private var token: IMediaCenterClientToken? = null
    private var mediaSessionCoordinator: MediaSessionCoordinator? = null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            svc = IMediaCenterSvc.Stub.asInterface(binder)
            isBound = true
            Log.i(TAG, "Service connected")
            mediaSessionCoordinator?.start()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            svc = null
            isBound = false
            Log.i(TAG, "Service disconnected")
        }
    }

    fun setMediaSessionCoordinator(coordinator: MediaSessionCoordinator) {
        mediaSessionCoordinator = coordinator
    }

    fun isConnected(): Boolean {
        Log.i(TAG, "isBound: $isBound")
        Log.i(TAG, "svc: $svc")
        Log.i(TAG, "alive: ${isServiceAlive()}")
        return isBound && svc != null && isServiceAlive()
    }

    private fun isServiceAlive(): Boolean {
        return try {
            svc?.asBinder()?.pingBinder() == true
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting service alive status: $e")
            false
        }
    }

    fun isConnectionInProgress(): Boolean {
        return isConnecting &&
                System.currentTimeMillis() - lastConnectionTime < CONNECTION_TIMEOUT_MS
    }

    fun start() {
        if (isConnected() || isConnectionInProgress()) return

        Log.i(TAG, "Binding to MediaCenterService started")
        isConnecting = true
        lastConnectionTime = System.currentTimeMillis()
        val intent = Intent(SERVICE_ACTION).apply {
            setComponent(ComponentName(SERVICE_PKG, SERVICE_CLS))
        }

        try {
            val bound = service.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            if (!bound) {
                Log.e(TAG, "Binding to MediaCenterService failed")
                isConnecting = false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Binding failed - missing permission?", e)
            isConnecting = false
        } catch (e: Exception) {
            Log.e(TAG, "Binding failed", e)
            isConnecting = false
        }
    }

    fun stop() {
        if (isBound) {
            try {
                updateWithPlaybackInfo(MusicPlaybackInfo().apply { playbackStatus = 2 } )
                service.unbindService(conn)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Service already unbound", e)
            }
            isBound = false
        }
        isConnecting = false
    }

    fun registerPlayer(pkg: String = "ru.yandex.music") {
        val svc = svc ?: return
        try {
            val musicClient = MusicClient(context)
            token = svc.registerInMusic(pkg, musicClient)
            if (token != null) {
                Log.i(TAG, "registered: $token")
                requestPlay()
            } else {
                Log.w(TAG, "registration rejected (whitelist / signature)")
            }
        } catch (e: RemoteException) {
            Log.w(TAG, "IPC failed", e)
        }
    }

    fun unregisterPlayer() {
        if (token == null) {
            return
        }
        svc?.unregister(token!!)
        token = null
    }

    fun requestPlay(): Boolean {
        return try {
            val currentToken = token ?: run {
                Log.w(TAG, "No token available for requestPlay")
                return false
            }

            svc?.let { service ->
                service.requestPlay(currentToken)
                service.updateCurrentSourceType(currentToken, 6)
                true
            } ?: run {
                Log.w(TAG, "Service not available for requestPlay")
                false
            }
        } catch (e: RemoteException) {
            Log.w(TAG, "IPC failed in requestPlay", e)
            false
        }
    }

    fun updateWithPlaybackInfo(playbackInfo: MusicPlaybackInfo) {
        requestPlay()
        token?.let { token ->
            try {
                svc?.updateMusicPlaybackState(token, playbackInfo)
                Log.i(TAG, "Updated playback info from Media3 session")
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to update playback info", e)
            }
        }
    }

    fun updateProgress(position: Long) {
        requestPlay()
        token?.let { token ->
            try {
                svc?.updateCurrentProgress(token, position)
                Log.d(TAG, "Updated progress: $position")
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to update progress", e)
            }
        }
    }
}