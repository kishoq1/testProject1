package com.example.testproject.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


object PlaybackState {
    private val _currentVideo = MutableStateFlow<Video?>(null)
    val currentVideo = _currentVideo.asStateFlow()

    private var currentVideoUrl: String? = null

    fun setCurrentVideo(title: String, url: String) {
        _currentVideo.value = Video(
            title = title,
            url = url,
            thumbnailUrl = ""
        )
        currentVideoUrl = url
    }
    
    fun clear() {
        _currentVideo.value = null
        currentVideoUrl = null
    }
}