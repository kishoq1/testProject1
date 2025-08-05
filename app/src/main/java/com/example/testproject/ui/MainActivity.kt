package com.example.testproject.ui
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.example.testproject.ui.screen.VideoSearchScreen
import com.example.testproject.ui.theme.YourAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            YourAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoSearchScreen(
                        onVideoClick = { video ->
                            // TODO: Mở màn hình VideoPlayerActivity hoặc xử lý video đã chọn
                            val intent = Intent(this, VideoPlayerActivity::class.java)
                            intent.putExtra("video_url", video.url)
                            intent.putExtra("video_title", video.title)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
