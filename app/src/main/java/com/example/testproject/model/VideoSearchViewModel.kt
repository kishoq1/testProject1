package com.example.testproject.model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testproject.model.Video
import com.example.testproject.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoSearchViewModel(
    private val repository: VideoRepository = VideoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    val searchResults: StateFlow<List<Video>> = _searchResults

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

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
