package com.example.testproject.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri

object VideoPlayerManager {
    private var player: ExoPlayer? = null

    fun initializePlayer(context: Context, videoUrl: String) {
        releasePlayer()
        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl.toUri()))
            prepare()
            playWhenReady = true
        }
    }

    fun getPlayer(): ExoPlayer? = player

    fun releasePlayer() {
        player?.release()
        player = null
    }
}
