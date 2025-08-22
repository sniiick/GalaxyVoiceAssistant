package com.example.voiceapp3.media

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.media.session.MediaButtonReceiver
import ecarx.xsf.mediacenter.MusicPlaybackInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest


object MetadataAdapter {
    fun toPlaybackInfo(controller: MediaController, context: Context): MusicPlaybackInfo {
        logAllMetadata(controller)
        return MusicPlaybackInfo().apply {
            val meta = controller.metadata ?: return@apply
            val state = controller.playbackState ?: return@apply
            val pkg = controller.packageName ?: "unknown"

            appName = controller.packageName
            packageName = controller.packageName
            iconUri = getPackageUri(pkg, context)
            playbackStatus = if (state.state == 3 || state.state == 6 || state.state == 8) 1 else 0

            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
            album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
            duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)

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
        Log.i("ARTWORK", "Loading artwork")

        // Check URI fields in priority order
        listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI
        ).forEach { key ->
            meta.getString(key)?.let { uriString ->
                Log.i("ARTWORK", "Got URI from $key: $uriString")
                return uriString.toUri()
            }
        }

        // If no URI found, check bitmap fields
        Log.i("ARTWORK", "No URI found, loading from bitmap")
        listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART
        ).forEach { key ->
            meta.getBitmap(key)?.let { bitmap ->
                return cacheArtworkBitmap(context, meta, bitmap)
            }
        }

        return Uri.EMPTY
    }

    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    private fun cacheArtworkBitmap(context: Context, meta: MediaMetadata, bmp: Bitmap): Uri {
        // Generate a unique but consistent filename based on media metadata
        val filename = generateArtworkFilename(meta)
        val dir = File(context.externalCacheDir, "art").apply { mkdirs() }
        val file = File(dir, filename)

        // Only save if file doesn't exist or is different
        if (!file.exists() || shouldOverwrite(file, bmp)) {
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }

            // Set file permissions (666 = RW-RW-RW-)
            file.setReadable(true, false)
            file.setWritable(true, false)
            file.setExecutable(false, false)


            Log.d("ARTWORK_CACHE", "Cached artwork: ${file.absolutePath}")
        } else {
            Log.d("ARTWORK_CACHE", "Using cached artwork: ${file.absolutePath}")
        }

        return Uri.fromFile(file)
    }

    private fun generateArtworkFilename(meta: MediaMetadata): String {
        // Create a hash-based filename using media metadata
        val uniqueId = buildString {
            append(meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: "")
            append(meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "")
            append(meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "")
            append(meta.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "")
        }

        return if (uniqueId.isNotEmpty()) {
            // Use MD5 hash for consistent filename
            val hash = md5(uniqueId)
            "art_$hash.jpg"
        } else {
            // Fallback to timestamp if no metadata
            "art_${System.currentTimeMillis()}.jpg"
        }
    }

    private fun shouldOverwrite(file: File, newBitmap: Bitmap): Boolean {
        // Optional: Add logic to check if existing file is different
        // For simplicity, we'll just overwrite if file exists
        return true // or implement bitmap comparison logic
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
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
            Log.d("MediaMetadata", "No metadata available")
            return
        }

        Log.d("MediaMetadata", "===== STANDARD METADATA =====")
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
            Log.d("MediaMetadata", "$label: ${metadata.getString(key) ?: "null"}")
        }
    }

    private fun logLongMetadata(metadata: MediaMetadata, key: String, label: String) {
        if (metadata.containsKey(key)) {
            Log.d("MediaMetadata", "$label: ${metadata.getLong(key)}")
        }
    }
}