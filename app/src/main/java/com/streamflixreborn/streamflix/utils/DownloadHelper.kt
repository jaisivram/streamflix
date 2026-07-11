package com.streamflixreborn.streamflix.utils

import android.content.Context
import android.content.Intent
import com.streamflixreborn.streamflix.models.Download
import com.streamflixreborn.streamflix.models.Movie
import com.streamflixreborn.streamflix.models.Episode
import com.streamflixreborn.streamflix.services.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DownloadHelper {
    
    suspend fun initiateMovieDownload(
        context: Context,
        movie: Movie,
        videoUrl: String
    ) = withContext(Dispatchers.IO) {
        val fileName = "${movie.title.replace(Regex("[^a-zA-Z0-9]"), "_")}.mp4"
        val download = DownloadManager.createDownload(
            contentId = movie.id,
            contentTitle = movie.title,
            contentType = "movie",
            videoUrl = videoUrl,
            fileName = fileName,
            context = context
        )
        
        if (download != null) {
            startDownloadService(context, download.id)
        }
        download
    }

    suspend fun initiateEpisodeDownload(
        context: Context,
        episode: Episode,
        videoUrl: String
    ) = withContext(Dispatchers.IO) {
        val fileName = "${episode.tvShow?.title ?: "Episode"}_S${episode.season?.number}_E${episode.number}.mp4"
            .replace(Regex("[^a-zA-Z0-9]"), "_")
        
        val download = DownloadManager.createDownload(
            contentId = episode.id,
            contentTitle = "${episode.tvShow?.title ?: "Unknown"} - ${episode.title ?: "Episode ${episode.number}"}",
            contentType = "episode",
            videoUrl = videoUrl,
            fileName = fileName,
            context = context,
            episodeNumber = episode.number,
            seasonNumber = episode.season?.number
        )
        
        if (download != null) {
            startDownloadService(context, download.id)
        }
        download
    }

    fun startDownloadService(context: Context, downloadId: Long) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
        }
        context.startService(intent)
    }

    fun pauseDownload(context: Context, downloadId: Long) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
        }
        context.startService(intent)
    }

    fun cancelDownload(context: Context, downloadId: Long) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
        }
        context.startService(intent)
    }

    fun getDownloadedFilePath(context: Context, download: Download): String? {
        return download.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) path else null
        }
    }
}
