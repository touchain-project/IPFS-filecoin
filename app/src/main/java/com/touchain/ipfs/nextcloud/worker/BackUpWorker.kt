package com.touchain.basic.module.nextcloud.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.touchain.basic.db.AppDatabase
import com.touchain.basic.module.nextcloud.CLOUD_LOCAL_PATH
import com.touchain.basic.module.nextcloud.CLOUD_REMOTE_PATH
import com.touchain.basic.module.nextcloud.CloudRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class BackUpWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), AnkoLogger {

    private val cloudRepository = CloudRepository.getInstance(
        AppDatabase.getInstance(context.applicationContext).cloudDao()
    )

    private val localPath = workerParams.inputData.getString(CLOUD_LOCAL_PATH) ?: ""
    private val remotePath = workerParams.inputData.getString(CLOUD_REMOTE_PATH) ?: ""
    private val listener: OnDatatransferProgressListener =
        OnDatatransferProgressListener { progressRate, totalTransferredSoFar,
                                         totalToTransfer, fileAbsoluteName ->
        }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val success = cloudRepository.uploadFile(remotePath, localPath, listener)
        if (success) {
            Result.success()
        } else {
            Result.failure()
        }
    }

}