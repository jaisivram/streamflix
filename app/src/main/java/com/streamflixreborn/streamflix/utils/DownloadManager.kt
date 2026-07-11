package com.streamflixreborn.streamflix.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.streamflixreborn.streamflix.database.AppDatabase
import com.streamflixreborn.streamflix.models.Download
import com.streamflixreborn.streamflix.database.dao.DownloadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object DownloadManager {
    private const val TAG = "DownloadManager"
    private const val DOWNLOAD_DIR = "StreamFlixDownloads"
    private var dao: DownloadDao? = null
    private val okHttpClient = OkHttpClient()

    fun initialize(context: Context) {
        if (dao == null) {
            try {
                dao = AppDatabase.getInstance(context).downloadDao()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize DownloadDao", e)
            }
        }
    }

    fun getDownloadDao(): DownloadDao? = dao

    fun getDownloadsDirectory(context: Context): File {
        val downloadsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_DIR
        )
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }

    suspend fun createDownload(
        contentId: String,
        contentTitle: String,
        contentType: String,
        videoUrl: String,
        fileName: String,
        context: Context,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null
    ): Download? = withContext(Dispatchers.IO) {
        return@withContext try {
            val download = Download(
                contentId = contentId,
                contentTitle = contentTitle,
                contentType = contentType,
                videoUrl = videoUrl,
                fileName = fileName,
                episodeNumber = episodeNumber,
                seasonNumber = seasonNumber,
                localPath = File(
                    getDownloadsDirectory(context),
                    fileName
                ).absolutePath
            )
            dao?.insert(download)
            download
        } catch (e: Exception) {
            Log.e(TAG, "Error creating download", e)
            null
        }
    }

    suspend fun updateDownloadProgress(
        downloadId: Long,
        downloadedBytes: Long,
        progress: Int,
        status: Download.DownloadStatus
    ) = withContext(Dispatchers.IO) {
        try {
            val download = dao?.getDownloadById(downloadId) ?: return@withContext
            val updated = download.copy(
                downloadedBytes = downloadedBytes,
                progress = progress,
                status = status
            )
            dao?.update(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating download progress", e)
        }
    }

    suspend fun completeDownload(
        downloadId: Long,
        localPath: String
    ) = withContext(Dispatchers.IO) {
        try {
            val download = dao?.getDownloadById(downloadId) ?: return@withContext
            val updated = download.copy(
                status = Download.DownloadStatus.COMPLETED,
                localPath = localPath,
                progress = 100,
                completedAtMillis = System.currentTimeMillis()
            )
            dao?.update(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing download", e)
        }
    }

    suspend fun failDownload(
        downloadId: Long,
        errorMessage: String
    ) = withContext(Dispatchers.IO) {
        try {
            val download = dao?.getDownloadById(downloadId) ?: return@withContext
            val updated = download.copy(
                status = Download.DownloadStatus.FAILED,
                errorMessage = errorMessage
            )
            dao?.update(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error failing download", e)
        }
    }

    suspend fun downloadVideoFile(
        videoUrl: String,
        outputPath: String,
        downloadId: Long,
        onProgress: (Long, Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = Request.Builder().url(videoUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val file = File(outputPath)
            file.parentFile?.mkdirs()

            val body = response.body ?: return@withContext Result.failure(
                Exception("Empty response body")
            )

            val totalBytes = body.contentLength()
            var downloadedBytes: Long = 0

            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                        onProgress(downloadedBytes, progress)
                    }
                }
            }

            response.close()
            Result.success(outputPath)
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDownload(downloadId: Long, context: Context) = withContext(Dispatchers.IO) {
        try {
            val download = dao?.getDownloadById(downloadId) ?: return@withContext
            download.localPath?.let { path ->
                File(path).delete()
            }
            dao?.deleteDownloadById(downloadId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download", e)
        }
    }

    fun getDownloadsByStatus(status: Download.DownloadStatus): Flow<List<Download>>? =
        dao?.getDownloadsByStatus(status)

    fun getAllDownloads(): Flow<List<Download>>? = dao?.getAllDownloads()

    fun getAllMovieDownloads(): Flow<List<Download>>? = dao?.getAllMovieDownloads()

    fun getAllEpisodeDownloads(): Flow<List<Download>>? = dao?.getAllEpisodeDownloads()
}
