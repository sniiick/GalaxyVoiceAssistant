package com.example.voiceapp3.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

/**
 * Pure listener that tells the outside world **which** controller is now
 * the one we should use (or null if nothing is active).
 */
class MediaSessionObserver(
    context: Context,
    private val onControllerChosen: (MediaController?) -> Unit
) : MediaSessionManager.OnActiveSessionsChangedListener {
    private val manager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val component =
        ComponentName(context, com.example.voiceapp3.VoiceAssistantService::class.java)

    fun start() = manager.addOnActiveSessionsChangedListener(this, component)
    fun stop()  = manager.removeOnActiveSessionsChangedListener(this)

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        val best = controllers?.maxWithOrNull(
            compareBy(
                { it.playbackState?.state == PlaybackState.STATE_PLAYING },
                { it.playbackState?.lastPositionUpdateTime ?: 0L })
        )
        onControllerChosen(best)
    }
}