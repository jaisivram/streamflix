package com.streamflixreborn.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.streamflixreborn.streamflix.models.Download
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert
    suspend fun insert(download: Download): Long

    @Update
    suspend fun update(download: Download)

    @Delete
    suspend fun delete(download: Download)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): Download?

    @Query("SELECT * FROM downloads WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getDownloadByContent(contentId: String, contentType: String): Download?

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAtMillis DESC")
    fun getDownloadsByStatus(status: Download.DownloadStatus): Flow<List<Download>>

    @Query("SELECT * FROM downloads ORDER BY createdAtMillis DESC")
    fun getAllDownloads(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE contentType = 'movie' ORDER BY createdAtMillis DESC")
    fun getAllMovieDownloads(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE contentType = 'episode' ORDER BY createdAtMillis DESC")
    fun getAllEpisodeDownloads(): Flow<List<Download>>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    fun getDownloadCountByStatus(status: Download.DownloadStatus): Flow<Int>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: Download.DownloadStatus)
}
