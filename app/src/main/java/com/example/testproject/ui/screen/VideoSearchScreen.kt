package com.example.testproject.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testproject.VideoList
import com.example.testproject.model.Video
import com.example.testproject.model.PlaybackState
import com.example.testproject.model.VideoSearchViewModel

@Composable
fun VideoSearchScreen(
    viewModel: VideoSearchViewModel = viewModel(),
    onVideoClick: (Video) -> Unit,
    onMiniPlayerClick: () -> Unit

) {
    val searchResults by viewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val currentlyPlayingVideo by PlaybackState.currentVideo.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
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

            VideoList(
                videos = searchResults,
                onVideoClick = onVideoClick,
                modifier = Modifier.padding(bottom = if (currentlyPlayingVideo != null) 64.dp else 0.dp)
            )
        }

        if (currentlyPlayingVideo != null) {
            MiniPlayerBar(
                video = currentlyPlayingVideo!!,
                onClick = onMiniPlayerClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đang phát: ${video.title}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}