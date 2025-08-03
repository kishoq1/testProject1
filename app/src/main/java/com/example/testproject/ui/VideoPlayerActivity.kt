package com.example.testproject.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.testproject.util.VideoPlayerManager
import com.example.testproject.ui.VideoPlayerScreen

class VideoPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        VideoPlayerManager.initializePlayer(this, videoUrl)

        setContent {
            VideoPlayerScreen(player = VideoPlayerManager.getPlayer())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoPlayerManager.releasePlayer()
    }
}
