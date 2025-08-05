package com.example.testproject.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testproject.VideoList
import com.example.testproject.model.Video
import com.example.testproject.model.VideoSearchViewModel

@Composable
fun VideoSearchScreen(
    viewModel: VideoSearchViewModel = viewModel(),
    onVideoClick: (Video) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Thanh tìm kiếm
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                placeholder = { Text("Tìm kiếm video...") },
                singleLine = true
            )
            Button(onClick = { viewModel.searchVideos(searchQuery) }) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        VideoList(videos = searchResults, onVideoClick = onVideoClick)
    }
}