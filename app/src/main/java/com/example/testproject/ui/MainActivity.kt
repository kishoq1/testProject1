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
                            // **BẮT ĐẦU THAY ĐỔI**
                            // Đóng Activity đang phát video cũ (nếu có) trước khi mở video mới.
                            VideoPlayerActivity.finishActivity()
                            // **KẾT THÚC THAY ĐỔI**

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