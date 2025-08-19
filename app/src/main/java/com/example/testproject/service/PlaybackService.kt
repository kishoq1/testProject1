package com.example.testproject.service

import android.app.PendingIntent
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.*
import com.example.testproject.model.PlaybackState
import com.example.testproject.ui.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val tAG = "PlaybackService_Stable"
    private val playbackHistory = mutableListOf<String>()


    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) serviceScope.launch { playNextVideo() }
        }


        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                val title = it.mediaMetadata.title?.toString() ?: "dang phát"
                val url = it.mediaId
                PlaybackState.setCurrentVideo(title, url)
            }
        }
    }

    private inner class CustomPlayer(player: Player) : ForwardingPlayer(player) {
        override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_PREVIOUS).build()
        override fun isCommandAvailable(command: Int): Boolean =
            (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) || super.isCommandAvailable(command)
        override fun seekToNext() { serviceScope.launch { playNextVideo() } }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun seekToPrevious() { serviceScope.launch { playPreviousVideo() } }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>)
                : ListenableFuture<MutableList<MediaItem>> {
            mediaItems.firstOrNull()?.let {
                val title = it.mediaMetadata.title?.toString() ?: "dang tải..."
                PlaybackState.setCurrentVideo(title, it.mediaId)
                serviceScope.launch { preparePlayerFor(it) }
            }
            return Futures.immediateFuture(mediaItems)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        TrafficStats.setThreadStatsTag(0xF00D)
        startForegroundService(Intent(this, PlaybackService::class.java))

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16 * 1024))
            .setBufferDurationsMs(
                30_000,
                120_000,
                2_000,
                5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)

            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(playerListener)
        val customPlayer = CustomPlayer(exoPlayer)

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
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


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
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
                val mergedSource = MergingMediaSource(true, videoMediaSource, audioMediaSource)

                withContext(Dispatchers.Main) {
                    exoPlayer.setMediaSource(mergedSource, true)
                    exoPlayer.prepare()
                }
            } catch (e: Exception) {
                Log.e(tAG, "Lỗi n trọng trong quá trình cbị trình phát", e)
            }
        }
    }

    private suspend fun playNextVideo() {
        val currentPageUrl = mediaSession?.player?.currentMediaItem?.mediaId ?: return
        val nextVideoUrl = getNextRelatedVideoUrl(currentPageUrl) ?: return
        val nextMediaItem = MediaItem.Builder().setMediaId(nextVideoUrl).build()
        val title = nextMediaItem.mediaMetadata.title?.toString() ?: "dang tải..."
        PlaybackState.setCurrentVideo(title, nextVideoUrl)
        preparePlayerFor(nextMediaItem)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun playPreviousVideo() {
        if (playbackHistory.size < 2) { mediaSession?.player?.seekTo(0); return }
        playbackHistory.removeLast()
        val previousVideoUrl = playbackHistory.removeLast()
        val prevMediaItem = MediaItem.Builder().setMediaId(previousVideoUrl).build()
        val title = prevMediaItem.mediaMetadata.title?.toString() ?: "dang tải..."
        PlaybackState.setCurrentVideo(title, previousVideoUrl)
        preparePlayerFor(prevMediaItem)
    }

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
        PlaybackState.clear()
    }

}