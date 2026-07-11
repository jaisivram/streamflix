package com.streamflixreborn.streamflix.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity("downloads")
data class Download(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val contentTitle: String,
    val contentType: String, // "movie" or "episode"
    val episodeNumber: Int? = null, // For TV episodes
    val seasonNumber: Int? = null, // For TV episodes
    val videoUrl: String,
    val fileName: String,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0, // 0-100
    val errorMessage: String? = null,
    val localPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null,
) : Serializable {
    enum class DownloadStatus {
        PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    }
}
