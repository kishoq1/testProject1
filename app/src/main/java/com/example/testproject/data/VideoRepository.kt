package com.example.testproject.data

import com.example.testproject.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoRepository {
    suspend fun searchVideos(query: String): List<Video> = withContext(Dispatchers.IO) {
        // Dữ liệu giả lập
        listOf(
            Video(
                title = "Kotlin Jetpack Compose Tutorial",
                url = "https://example.com/video1",
                thumbnailUrl = "https://via.placeholder.com/150"
            ),
            Video(
                title = "Jetpack Compose Basics",
                url = "https://example.com/video2",
                thumbnailUrl = "https://via.placeholder.com/150"
            )
        ).filter { it.title.contains(query, ignoreCase = true) }
    }
}

