package com.example.testproject.ui

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.testproject.service.PlaybackService
import com.example.testproject.ui.theme.YourAppTheme
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList

class VideoPlayerActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageUrl = intent.getStringExtra("video_url")
        // NHẬN TIÊU ĐỀ TỪ INTENT
        val videoTitle = intent.getStringExtra("video_title")

        setContent {
            YourAppTheme {
                var player by remember { mutableStateOf<Player?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(this@VideoPlayerActivity, ComponentName(this@VideoPlayerActivity, PlaybackService::class.java))
                    controllerFuture = MediaController.Builder(this@VideoPlayerActivity, sessionToken).buildAsync()

                    controllerFuture?.addListener({
                        val connectedController = this@VideoPlayerActivity.controller ?: return@addListener
                        player = connectedController

                        if (!pageUrl.isNullOrEmpty()) {
                            lifecycleScope.launch {
                                val streamUrl = extractStreamUrl(pageUrl)
                                if (streamUrl != null) {
                                    // --- THAY ĐỔI QUAN TRỌNG Ở ĐÂY ---
                                    // 1. Tạo metadata với tiêu đề
                                    val mediaMetadata = MediaMetadata.Builder()
                                        .setTitle(videoTitle)
                                        .build()

                                    // 2. Tạo MediaItem và gán metadata vào
                                    val mediaItem = MediaItem.Builder()
                                        .setUri(streamUrl)
                                        .setMediaId(pageUrl)
                                        .setMediaMetadata(mediaMetadata) // Gán metadata
                                        .setRequestMetadata(
                                            MediaItem.RequestMetadata.Builder()
                                                .setMediaUri(pageUrl.toUri())
                                                .build()
                                        )
                                        .build()

                                    connectedController.setMediaItem(mediaItem)
                                    connectedController.prepare()
                                    connectedController.play()
                                }
                                isLoading = false
                            }
                        } else {
                            isLoading = false
                        }
                    }, ContextCompat.getMainExecutor(this@VideoPlayerActivity))

                    onDispose {
                        controllerFuture?.let { MediaController.releaseFuture(it) }
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (player != null) {
                        VideoPlayerScreen(player = player)
                    }
                }
            }
        }
    }

    private suspend fun extractStreamUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()
                extractor.videoStreams.minByOrNull {
                    val resolution = it.resolution.replace("p", "").toIntOrNull() ?: 0
                    kotlin.math.abs(resolution - 720)
                }?.url
            } catch (e: Exception) {
                Log.e("VideoPlayerActivity", "Failed to extract URL", e)
                null
            }
        }
    }
}