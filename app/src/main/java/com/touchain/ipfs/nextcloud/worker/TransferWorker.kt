package com.touchain.basic.module.nextcloud.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.touchain.basic.db.AppDatabase
import com.touchain.basic.db.entity.TransferData
import com.touchain.basic.module.nextcloud.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class TransferWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), AnkoLogger {

    private val cloudRepository = CloudRepository.getInstance(
        AppDatabase.getInstance(context.applicationContext).cloudDao()
    )

    private val localPath = workerParams.inputData.getString(CLOUD_LOCAL_PATH) ?: ""
    private val remotePath = workerParams.inputData.getString(CLOUD_REMOTE_PATH) ?: ""
    private val type =
        workerParams.inputData.getInt(
            CLOUD_TRANSFER_TYPE,
            CLOUD_TRANSFER_TYPE_DOWNLOAD
        )

    private val transferDao = AppDatabase.getInstance(context.applicationContext).transferDao()

    private val listener: OnDatatransferProgressListener =
        OnDatatransferProgressListener { progressRate, totalTransferredSoFar,
                                         totalToTransfer, fileAbsoluteName ->
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                val percentage =
                    if (totalToTransfer > 0) totalTransferredSoFar * 100 / totalToTransfer else 0
                setTransferProgress(percentage.toInt())
            }
        }

    private fun getProgressData(progress: Int): Data =
        workDataOf(
            CLOUD_WORKER_PROGRESS to progress,
            CLOUD_REMOTE_PATH to remotePath
        )

    private suspend fun setTransferProgress(progress: Int) {
        setProgress(getProgressData(progress))
        transferDao.getTransferDataByPath(remotePath)?.run {
            val progressItem = TransferData(
                remotePath, localPath, displayName, fileType,
                transferType, modifiedTimestamp, progress
            )
            transferDao.insertTransferData(progressItem)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            info("doWork  $type localPath: $localPath   remotePath: $remotePath ")
            setTransferProgress(0)
            val isTransfer = if (CLOUD_TRANSFER_TYPE_UPLOAD == type) {
                cloudRepository.uploadFile(remotePath, localPath, listener)
            } else {
                cloudRepository.downloadFiles(remotePath, localPath, listener)
            }
            info("doWork   isTransfer: $isTransfer ")

            if (isTransfer) {
                setTransferProgress(100)
                Result.success()
            } else {
                Result.failure()
            }

        } catch (ex: Exception) {
            Result.failure()
        }
    }
}