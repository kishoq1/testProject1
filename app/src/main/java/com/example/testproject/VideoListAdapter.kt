package com.example.testproject

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.testproject.model.Video

@Composable
fun VideoList(videos: List<Video>, onVideoClick: (Video) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(videos) { video ->
            VideoListItem(video = video, onClick = { onVideoClick(video) })
        }
    }
}

@Composable
fun VideoListItem(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Thay đổi từ Row sang Column để xếp các thành phần theo chiều dọc
        Column(modifier = Modifier.fillMaxWidth()) {
            // Chỉnh sửa Image để chiếm toàn bộ chiều rộng và có tỷ lệ khung hình 16:9
            Image(
                painter = rememberAsyncImagePainter(video.thumbnailUrl),
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f), // Đặt tỷ lệ khung hình cho ảnh
                contentScale = ContentScale.Crop // Căn chỉnh ảnh để lấp đầy khung
            )
            // Thêm khoảng cách giữa ảnh và tiêu đề
            Spacer(modifier = Modifier.height(8.dp))
            // Hiển thị tiêu đề bên dưới ảnh
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp) // Thêm padding cho tiêu đề
                    .padding(bottom = 8.dp)
            )
        }
    }
}