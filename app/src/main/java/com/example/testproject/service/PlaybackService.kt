package com.example.testproject.service



import android.content.Intent

import android.util.Log

import androidx.media3.common.MediaItem

import androidx.media3.common.MediaMetadata

import androidx.media3.common.Player

import androidx.media3.common.util.UnstableApi

import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.session.MediaSession

import androidx.media3.session.MediaSessionService

import kotlinx.coroutines.*

import org.schabi.newpipe.extractor.ServiceList

import org.schabi.newpipe.extractor.stream.StreamInfoItem



@UnstableApi

class PlaybackService : MediaSessionService() {



    private var mediaSession: MediaSession? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())



// Biến theo dõi index, được đưa ra ngoài làm biến thành viên của class

    private var previousIndex = 0



    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {

            if (playbackState == Player.STATE_ENDED) {

// Tự động phát khi kết thúc vẫn hoạt động

                serviceScope.launch { playNextVideo() }

            }

        }



// Triển khai ý tưởng của bạn một cách chính xác

        override fun onEvents(player: Player, events: Player.Events) {

// Kiểm tra xem có sự kiện chuyển bài hát (media item) hay không

            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {

                val currentIndex = player.currentMediaItemIndex

// Logic này chỉ nên áp dụng cho playlist có nhiều item,

// nhưng chúng ta có thể tạm dùng để xử lý nút next/previous

                if (currentIndex > previousIndex) {

// Người dùng có thể đã nhấn "Next"

// (Lưu ý: Logic này sẽ được cải tiến)

                    Log.d("PlaybackService", "Next action detected")

                    serviceScope.launch { playNextVideo() }

                } else if (currentIndex < previousIndex) {

// Người dùng có thể đã nhấn "Previous"

                    Log.d("PlaybackService", "Previous action detected")

                    player.seekTo(0)

                }

// Cập nhật lại index sau khi xử lý

                previousIndex = currentIndex

            }

        }

    }



    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession



    override fun onCreate() {

        super.onCreate()

        val player = ExoPlayer.Builder(this).build().apply {

            addListener(playerListener)

        }



        mediaSession = MediaSession.Builder(this, player).build()

    }



    override fun onTaskRemoved(rootIntent: Intent?) {

        val player = mediaSession?.player ?: return

        if (!player.playWhenReady || player.mediaItemCount == 0) {

            stopSelf()

        }

    }



// ... (Toàn bộ các hàm còn lại không thay đổi) ...



    private suspend fun playNextVideo() {

        val currentPlayer = mediaSession?.player ?: return

        val currentMediaItem = currentPlayer.currentMediaItem ?: return

        val currentPageUrl = currentMediaItem.mediaId



        Log.d("PlaybackService", "Finding next video for: $currentPageUrl")

        val nextVideoUrl = getNextRelatedVideoUrl(currentPageUrl)

        if (nextVideoUrl == null) {

            Log.w("PlaybackService", "No related video found.")

            return

        }



        val nextStream = extractStreamInfo(nextVideoUrl)

        if (nextStream == null) {

            Log.e("PlaybackService", "Could not extract stream for next video.")

            return

        }



        Log.d("PlaybackService", "Playing next video: ${nextStream.second}")

        val nextMediaItem = MediaItem.Builder()

            .setUri(nextStream.first)

            .setMediaId(nextVideoUrl)

            .setMediaMetadata(MediaMetadata.Builder().setTitle(nextStream.second).build())

            .build()



        currentPlayer.setMediaItem(nextMediaItem)

        currentPlayer.prepare()

        currentPlayer.play()

    }



    private suspend fun getNextRelatedVideoUrl(url: String): String? {

        return withContext(Dispatchers.IO) {

            try {

                val extractor = ServiceList.YouTube.getStreamExtractor(url)

                extractor.fetchPage()

                val items = extractor.relatedItems?.items

// Sửa lại để an toàn hơn, không dùng "!!"

                if (items != null && items.isNotEmpty()) {

                    (items[0] as? StreamInfoItem)?.url

                } else {

                    null

                }

            } catch (e: Exception) {

                Log.e("PlaybackService", "Failed to get related items", e)

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

                Log.e("PlaybackService", "Failed to extract stream info", e)

                null

            }

        }

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