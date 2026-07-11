package com.streamflixreborn.streamflix.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.streamflixreborn.streamflix.models.Download
import com.streamflixreborn.streamflix.utils.DownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadService : Service() {
    companion object {
        const val ACTION_START_DOWNLOAD = "com.streamflixreborn.streamflix.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.streamflixreborn.streamflix.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.streamflixreborn.streamflix.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "download_id"
        const val TAG = "DownloadService"
    }

    private val binder = DownloadBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = mutableMapOf<Long, Boolean>()

    override fun onCreate() {
        super.onCreate()
        DownloadManager.initialize(this)
        Log.d(TAG, "DownloadService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    startDownload(downloadId)
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    pauseDownload(downloadId)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    cancelDownload(downloadId)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startDownload(downloadId: Long) {
        if (activeDownloads[downloadId] == true) {
            Log.w(TAG, "Download $downloadId already in progress")
            return
        }

        scope.launch {
            try {
                val dao = DownloadManager.getDownloadDao() ?: return@launch
                val download = dao.getDownloadById(downloadId) ?: return@launch

                activeDownloads[downloadId] = true
                DownloadManager.updateDownloadProgress(
                    downloadId,
                    0,
                    0,
                    Download.DownloadStatus.DOWNLOADING
                )

                val result = DownloadManager.downloadVideoFile(
                    download.videoUrl,
                    download.localPath ?: return@launch,
                    downloadId
                ) { downloadedBytes, progress ->
                    if (activeDownloads[downloadId] == true) {
                        scope.launch {
                            DownloadManager.updateDownloadProgress(
                                downloadId,
                                downloadedBytes,
                                progress,
                                Download.DownloadStatus.DOWNLOADING
                            )
                        }
                    }
                }

                if (result.isSuccess) {
                    DownloadManager.completeDownload(
                        downloadId,
                        result.getOrNull() ?: return@launch
                    )
                    Log.d(TAG, "Download $downloadId completed successfully")
                } else {
                    DownloadManager.failDownload(
                        downloadId,
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                    Log.e(TAG, "Download $downloadId failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in download $downloadId", e)
                DownloadManager.failDownload(downloadId, e.message ?: "Unknown error")
            } finally {
                activeDownloads[downloadId] = false
            }
        }
    }

    private fun pauseDownload(downloadId: Long) {
        activeDownloads[downloadId] = false
        scope.launch {
            DownloadManager.updateDownloadProgress(
                downloadId,
                0,
                0,
                Download.DownloadStatus.PAUSED
            )
            Log.d(TAG, "Download $downloadId paused")
        }
    }

    private fun cancelDownload(downloadId: Long) {
        activeDownloads[downloadId] = false
        scope.launch {
            try {
                val dao = DownloadManager.getDownloadDao() ?: return@launch
                val download = dao.getDownloadById(downloadId) ?: return@launch
                
                download.localPath?.let { path ->
                    java.io.File(path).delete()
                }
                
                dao.delete(download)
                Log.d(TAG, "Download $downloadId cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling download $downloadId", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            activeDownloads.forEach { (downloadId, isActive) ->
                if (isActive) {
                    pauseDownload(downloadId)
                }
            }
        }
        Log.d(TAG, "DownloadService destroyed")
    }

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }
}
