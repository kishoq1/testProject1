package com.example.testproject.ui

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.testproject.service.PlaybackService
import com.example.testproject.ui.theme.YourAppTheme
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class VideoPlayerActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageUrl = intent.getStringExtra("video_url")
        val videoTitle = intent.getStringExtra("video_title")

        setContent {
            YourAppTheme {
                var player by remember { mutableStateOf<Player?>(null) }

                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(this@VideoPlayerActivity, ComponentName(this@VideoPlayerActivity, PlaybackService::class.java))
                    controllerFuture = MediaController.Builder(this@VideoPlayerActivity, sessionToken).buildAsync()

                    controllerFuture?.addListener({
                        val connectedController = this@VideoPlayerActivity.controller ?: return@addListener

                        player = connectedController

                        if (!pageUrl.isNullOrEmpty()) {
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(videoTitle)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(pageUrl)
                                .setMediaMetadata(mediaMetadata)
                                .build()

                            connectedController.setMediaItem(mediaItem)
                            connectedController.playWhenReady = true
                        }
                    }, ContextCompat.getMainExecutor(this@VideoPlayerActivity))

                    onDispose {
                        controllerFuture?.let { MediaController.releaseFuture(it) }
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (player == null) {
                        CircularProgressIndicator()
                    } else {
                        VideoPlayerScreen(player = player)
                    }
                }
            }
        }
    }
}