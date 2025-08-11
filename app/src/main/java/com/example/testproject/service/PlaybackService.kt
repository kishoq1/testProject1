package com.example.testproject.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.*
import com.example.testproject.ui.VideoPlayerActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.upstream.DefaultAllocator

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val tAG = "PlaybackService_FinalFix"
    private val playbackHistory = mutableListOf<String>()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) serviceScope.launch { playNextVideo() }
        }
    }

    private inner class CustomPlayer(player: Player) : ForwardingPlayer(player) {
        override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_PREVIOUS).build()
        override fun isCommandAvailable(command: Int): Boolean =
            (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) || super.isCommandAvailable(command)
        override fun seekToNext() { serviceScope.launch { playNextVideo() } }
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        override fun seekToPrevious() { serviceScope.launch { playPreviousVideo() } }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>)
                : ListenableFuture<MutableList<MediaItem>> {
            mediaItems.firstOrNull()?.let { serviceScope.launch { preparePlayerFor(it) } }
            return Futures.immediateFuture(mediaItems)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16 * 1024)) // Cấp phát bộ nhớ
            .setBufferDurationsMs(
                32 * 1024, // minBufferMs: Tối thiểu 32 giây
                64 * 1024, // maxBufferMs: Tối đa 64 giây
                10 * 1024, // bufferForPlaybackMs: Cần 10 giây để bắt đầu phát
                5 * 1024   // bufferForPlaybackAfterRebufferMs: Cần 5 giây sau khi buffer lại
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()


        val exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true).build()
        exoPlayer.addListener(playerListener)
        val customPlayer = CustomPlayer(exoPlayer)

        val sessionActivityIntent = Intent(this, VideoPlayerActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, customPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(CustomMediaSessionCallback()).build()
    }

    private suspend fun preparePlayerFor(mediaItem: MediaItem) {
        val pageUrl = mediaItem.mediaId
        val player = mediaSession?.player as? ForwardingPlayer ?: return
        val exoPlayer = player.wrappedPlayer as? ExoPlayer ?: return

        if (playbackHistory.lastOrNull() != pageUrl) {
            playbackHistory.add(pageUrl)
            if (playbackHistory.size > 20) { playbackHistory.removeAt(0) }
        }

        withContext(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getStreamExtractor(pageUrl)
                extractor.fetchPage()
                val title = extractor.name

                val videoStream = extractor.videoStreams.filter { it.isVideoOnly() }
                    .maxByOrNull { if (it.getResolution().contains("480")) 1 else 0 }
                    ?: extractor.videoStreams.maxByOrNull { it.getResolution().replace("p","").toIntOrNull() ?: 0 }
                val audioStream = extractor.audioStreams
                    .maxByOrNull { if (it.format!!.name.contains("M4A")) 1 else 0 }
                    ?: extractor.audioStreams.maxByOrNull { it.averageBitrate }

                if (videoStream == null || audioStream == null) { return@withContext }

                val videoItemWithMetadata = MediaItem.Builder()
                    .setUri(videoStream.content!!)
                    .setMediaId(pageUrl)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()

                val audioItem = MediaItem.fromUri(audioStream.content!!)

                val dataSourceFactory = ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
                val videoMediaSource = dataSourceFactory.createMediaSource(videoItemWithMetadata)
                val audioMediaSource = dataSourceFactory.createMediaSource(audioItem)
                val mergedSource = MergingMediaSource(videoMediaSource, audioMediaSource)

                withContext(Dispatchers.Main) {
                    exoPlayer.setMediaSource(mergedSource, true)
                    exoPlayer.prepare()
                }
            } catch (e: Exception) {
                Log.e(tAG, "Lỗi nghiêm trọng trong quá trình chuẩn bị trình phát", e)
            }
        }
    }
    //Hàm phát video tiếp theo
    private suspend fun playNextVideo() {
        val currentPageUrl = mediaSession?.player?.currentMediaItem?.mediaId ?: return
        val nextVideoUrl = getNextRelatedVideoUrl(currentPageUrl) ?: return
        preparePlayerFor(MediaItem.Builder().setMediaId(nextVideoUrl).build())
    }
    //Hàm phát video trước đó
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun playPreviousVideo() {
        if (playbackHistory.size < 2) { mediaSession?.player?.seekTo(0); return }
        playbackHistory.removeLast()
        val previousVideoUrl = playbackHistory.removeLast()
        preparePlayerFor(MediaItem.Builder().setMediaId(previousVideoUrl).build())
    }
    //Hàm lấy video có nội dung tương tự
    private suspend fun getNextRelatedVideoUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                ServiceList.YouTube.getStreamExtractor(url).apply { fetchPage() }
                    .relatedItems?.items?.filterIsInstance<StreamInfoItem>()?.firstOrNull()?.url
            } catch (e: Exception) { null }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSession?.run { player.release(); release(); mediaSession = null }
    }
}