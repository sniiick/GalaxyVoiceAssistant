package com.example.voiceapp3.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.media.session.MediaButtonReceiver
import ecarx.xsf.mediacenter.MusicPlaybackInfo
import java.io.File
import java.io.FileOutputStream


object MetadataAdapter {
    fun toPlaybackInfo(controller: MediaController, context: Context): MusicPlaybackInfo {
        logAllMetadata(controller)
        return MusicPlaybackInfo().apply {
            val meta = controller.metadata ?: return@apply
            val state = controller.playbackState ?: return@apply
            val pkg = controller.packageName ?: "unknown"

            appName = "YandexMusic"
            packageName = controller.packageName
            iconUri = getPackageUri(pkg, context)
            playbackStatus = if (state.state == 3 || state.state == 6) 1 else 0

            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
            album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
            duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)

            if (duration == -1L) {
                sourceType = 12 // radio
                radioStationName = meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION).orEmpty()
            } else {
                sourceType = 6 // online music

            }
            meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)?.let {
                if (it.endsWith(".lrc")) lyric = it.toUri()
            }

            lyricContent = meta.getString("android.media.metadata.LYRICS").orEmpty()
            lyricSentence = meta.getString("android.media.metadata.CURRENT_LYRIC").orEmpty()

            artwork = loadArtworkUri(meta, context)
            launchIntent = controller.sessionActivity
            playerIntent = createMediaButtonPendingIntent(context)
        }
    }


    private fun loadArtworkUri(meta: MediaMetadata, context: Context): Uri {
        // 1. URI string
        meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?.let { return it.toUri() }

        // 2. Bitmap
        val bmp = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: return Uri.EMPTY

        return cacheBitmap(context, bmp)
    }

    private fun cacheBitmap(context: Context, bmp: Bitmap): Uri {
        val dir = File(context.externalCacheDir, "art").apply { mkdirs() }
        val file = File(dir, "art_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return Uri.fromFile(file)
    }

    fun getPackageUri(packageName: String, context: Context): String {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        val iconResId = appInfo.icon.takeIf { it != 0 } ?: android.R.drawable.sym_def_app_icon
        return "android.resource://$packageName/$iconResId".toUri().toString()
    }

    private fun createMediaButtonPendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            0,
            Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = ComponentName(ctx, MediaButtonReceiver::class.java)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun logAllMetadata(controller: MediaController) {
        val metadata = controller.metadata ?: run {
            Log.i("MediaMetadata", "No metadata available")
            return
        }

        Log.i("MediaMetadata", "===== STANDARD METADATA =====")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_TITLE, "Title")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_ARTIST, "Artist")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_ALBUM, "Album")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_ALBUM_ARTIST, "Album Artist")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_DISPLAY_TITLE, "Display Title")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "Display Subtitle")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, "Display Description")
        logLongMetadata(metadata, MediaMetadata.METADATA_KEY_DURATION, "Duration")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_GENRE, "Genre")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_DATE, "Date")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_WRITER, "Writer")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_COMPOSER, "Composer")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_COMPILATION, "Compilation")
        logLongMetadata(metadata, MediaMetadata.METADATA_KEY_TRACK_NUMBER, "Track Number")
        logLongMetadata(metadata, MediaMetadata.METADATA_KEY_NUM_TRACKS, "Total Tracks")
        logLongMetadata(metadata, MediaMetadata.METADATA_KEY_DISC_NUMBER, "Disc Number")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_YEAR, "Year")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_ALBUM_ART_URI, "Album Art URI")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_ART_URI, "Art URI")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, "Display Icon URI")
        logStringMetadata(metadata, MediaMetadata.METADATA_KEY_MEDIA_URI, "Media URI")
    }

    private fun logStringMetadata(metadata: MediaMetadata, key: String, label: String) {
        if (metadata.containsKey(key)) {
            Log.i("MediaMetadata", "$label: ${metadata.getString(key) ?: "null"}")
        }
    }

    private fun logLongMetadata(metadata: MediaMetadata, key: String, label: String) {
        if (metadata.containsKey(key)) {
            Log.i("MediaMetadata", "$label: ${metadata.getLong(key)}")
        }
    }
}