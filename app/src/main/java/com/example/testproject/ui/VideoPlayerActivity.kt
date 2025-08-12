package com.example.testproject.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.testproject.VideoListItem
import com.example.testproject.data.VideoRepository
import com.example.testproject.model.Video
import com.example.testproject.service.PlaybackService
import com.example.testproject.ui.theme.YourAppTheme
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class VideoPlayerActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    @SuppressLint("ContextCastToActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialPageUrl = intent.getStringExtra("video_url")
        val initialVideoTitle = intent.getStringExtra("video_title") ?: "Video"
        val isFirstTimeCreated = savedInstanceState == null

        setContent {
            YourAppTheme {
                var player by remember { mutableStateOf<Player?>(null) }
                var isFullScreen by rememberSaveable { mutableStateOf(false) }
                var videoAspectRatio by remember { mutableFloatStateOf(16 / 9f) }

                var currentTitle by rememberSaveable { mutableStateOf(initialVideoTitle) }
                var relatedVideos by remember { mutableStateOf<List<Video>>(emptyList()) }

                val context = LocalContext.current as ComponentActivity

                BackHandler(enabled = isFullScreen) {
                    isFullScreen = false
                }

                LaunchedEffect(isFullScreen) {
                    context.requestedOrientation = if (isFullScreen) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }

                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(this@VideoPlayerActivity, ComponentName(this@VideoPlayerActivity, PlaybackService::class.java))
                    controllerFuture = MediaController.Builder(this@VideoPlayerActivity, sessionToken).buildAsync()

                    val playerListener = object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            if (videoSize.height > 0) {
                                videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                            }
                        }

                        override fun onEvents(player: Player, events: Player.Events) {
                            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                                player.currentMediaItem?.let { mediaItem ->
                                    currentTitle = mediaItem.mediaMetadata.title.toString()
                                    lifecycleScope.launch {
                                        relatedVideos = VideoRepository.getRelatedVideos(mediaItem.mediaId)
                                    }
                                }
                            }
                        }
                    }

                    controllerFuture?.addListener({
                        val connectedController = this@VideoPlayerActivity.controller ?: return@addListener
                        player = connectedController
                        connectedController.addListener(playerListener)

                        if (isFirstTimeCreated && !initialPageUrl.isNullOrEmpty()) {
                            val mediaMetadata = MediaMetadata.Builder().setTitle(currentTitle).build()
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(initialPageUrl)
                                .setMediaMetadata(mediaMetadata)
                                .build()
                            connectedController.setMediaItem(mediaItem)
                            connectedController.playWhenReady = true
                            connectedController.prepare()

                            // Tải danh sách liên quan lần đầu
                            lifecycleScope.launch {
                                relatedVideos = VideoRepository.getRelatedVideos(initialPageUrl)
                            }
                        }

                    }, ContextCompat.getMainExecutor(this@VideoPlayerActivity))

                    onDispose {
                        controller?.removeListener(playerListener)
                        controllerFuture?.let { MediaController.releaseFuture(it) }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (isFullScreen) {
                        VideoPlayerScreen(
                            player = player,
                            isFullScreen = true,
                            onToggleFullScreen = { isFullScreen = false }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            VideoPlayerScreen(
                                player = player,
                                isFullScreen = false,
                                onToggleFullScreen = { isFullScreen = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(videoAspectRatio)
                            )
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                item {
                                    Text(
                                        text = currentTitle,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(relatedVideos) { video ->
                                    VideoListItem(
                                        video = video,
                                        onClick = {
                                            val mediaMetadata = MediaMetadata.Builder().setTitle(video.title).build()
                                            val mediaItem = MediaItem.Builder()
                                                .setMediaId(video.url)
                                                .setMediaMetadata(mediaMetadata)
                                                .build()
                                            player?.setMediaItem(mediaItem)
                                            player?.prepare()
                                            player?.play()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}