package com.example.testproject.data

import android.util.Log
import com.example.testproject.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object VideoRepository {

    suspend fun searchVideos(query: String): List<Video> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val extractor = ServiceList.YouTube.getSearchExtractor(query)
            extractor.fetchPage()

            val results = extractor.initialPage.items.mapNotNull { item ->
                if (item is StreamInfoItem) {
                    val thumbnailUrl = item.getThumbnails().firstOrNull()?.url ?: ""
                    Video(
                        title = item.name,
                        url = item.url,
                        thumbnailUrl = thumbnailUrl
                    )
                } else {
                    null
                }
            }
            return@withContext results

        } catch (e: Exception) {
            Log.e("VideoRepository", "Error searching for videos", e)
            return@withContext emptyList<Video>()
        }
    }

    suspend fun getRelatedVideos(url: String): List<Video> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()

            val relatedVideos = extractor.relatedItems!!.items.mapNotNull { item ->
                if (item is StreamInfoItem) {
                    val thumbnailUrl = item.getThumbnails().firstOrNull()?.url ?: ""
                    Video(
                        title = item.name,
                        url = item.url,
                        thumbnailUrl = thumbnailUrl
                    )
                } else {
                    null
                }
            }
            return@withContext relatedVideos

        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching related videos", e)
            return@withContext emptyList()
        }
    }
}