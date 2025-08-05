package com.example.testproject.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testproject.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoSearchViewModel(
    private val repository: VideoRepository = VideoRepository
) : ViewModel() {

    // Giữ lại phần kết quả tìm kiếm
    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    val searchResults: StateFlow<List<Video>> = _searchResults

    // Giữ lại hàm tìm kiếm
    fun searchVideos(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val results = repository.searchVideos(trimmedQuery)
                    _searchResults.value = results
                } catch (e: Exception) {
                    _searchResults.value = emptyList()
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }
}