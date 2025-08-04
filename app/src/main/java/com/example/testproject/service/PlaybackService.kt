package com.example.testproject.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.C // Quan trọng: Thêm import này
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.example.testproject.ui.MainActivity
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val playbackHistory = mutableListOf<String>()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                serviceScope.launch { playNextVideo() }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let {
                if (playbackHistory.lastOrNull() != it) {
                    playbackHistory.add(it)
                    if (playbackHistory.size > 20) {
                        playbackHistory.removeAt(0)
                    }
                }
            }
        }
    }

    // Lớp Player tùy chỉnh để kiểm soát giao diện thông báo
    private inner class CustomPlayer(player: Player) : ForwardingPlayer(player) {

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
        }

        override fun isCommandAvailable(command: @Player.Command Int): Boolean {
            if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) {
                return true
            }
            return super.isCommandAvailable(command)
        }


        override fun seekToNext() {
            serviceScope.launch { playNextVideo() }
        }

        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        override fun seekToPrevious() {
            serviceScope.launch { playPreviousVideo() }
        }
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(playerListener)

        val customPlayer = CustomPlayer(exoPlayer)

        mediaSession = MediaSession.Builder(this, customPlayer)
            .setSessionActivity(getSingleTopActivity())
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private suspend fun playVideoByUrl(pageUrl: String) {
        val currentPlayer = (mediaSession?.player as? ForwardingPlayer)?.wrappedPlayer ?: return

        val streamInfo = extractStreamInfo(pageUrl)
        if (streamInfo == null) {
            Log.e("PlaybackService", "Không thể trích xuất thông tin stream cho URL: $pageUrl")
            return
        }

        Log.d("PlaybackService", "Đang phát video: ${streamInfo.second}")
        val mediaItem = MediaItem.Builder()
            .setUri(streamInfo.first)
            .setMediaId(pageUrl)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(streamInfo.second).build())
            .build()

        currentPlayer.setMediaItem(mediaItem)
        currentPlayer.prepare()
        currentPlayer.play()
    }

    private suspend fun playNextVideo() {
        val currentMediaItem = mediaSession?.player?.currentMediaItem ?: return
        val currentPageUrl = currentMediaItem.mediaId

        Log.d("PlaybackService", "Đang tìm video tiếp theo cho: $currentPageUrl")
        val nextVideoUrl = getNextRelatedVideoUrl(currentPageUrl)
        if (nextVideoUrl == null) {
            Log.w("PlaybackService", "Không tìm thấy video liên quan.")
            return
        }

        playVideoByUrl(nextVideoUrl)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun playPreviousVideo() {
        if (playbackHistory.size < 2) {
            Log.w("PlaybackService", "Không có video trước đó trong lịch sử.")
            mediaSession?.player?.seekTo(0)
            return
        }

        playbackHistory.removeLast()
        val previousVideoUrl = playbackHistory.removeLast()

        Log.d("PlaybackService", "Đang quay lại video trước đó: $previousVideoUrl")
        playVideoByUrl(previousVideoUrl)
    }

    private suspend fun getNextRelatedVideoUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()
                extractor.relatedItems?.items?.filterIsInstance<StreamInfoItem>()?.firstOrNull()?.url
            } catch (e: Exception) {
                Log.e("PlaybackService", "Lỗi khi lấy video liên quan", e)
                null
            }
        }
    }

    private suspend fun extractStreamInfo(url: String): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()
                val streamUrl = extractor.videoStreams.minByOrNull {
                    val resolution = it.resolution.replace("p", "").toIntOrNull() ?: 0
                    kotlin.math.abs(resolution - 720)
                }?.url

                if (streamUrl != null) {
                    Pair(streamUrl, extractor.name)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("PlaybackService", "Lỗi khi trích xuất thông tin stream", e)
                null
            }
        }
    }

    private fun getSingleTopActivity(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}