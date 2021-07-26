package com.touchain.basic.module.nextcloud

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import com.touchain.basic.db.dao.CloudDao
import com.touchain.basic.db.dao.TransferDao
import com.touchain.basic.db.entity.TransferData
import com.touchain.basic.ui.clouddisk.local.LocalFileItem
import com.touchain.basic.module.nextcloud.worker.TransferWorker
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CloudLocalRepository(
    private val transferDao: TransferDao,
    private val cloudDao: CloudDao
) : AnkoLogger {

    suspend fun uploadFiles(
        context: Context,
        uploadPath: String,
        localFiles: List<LocalFileItem>
    ) {
        //info( "uploadFiles  $uploadPath   localFiles： ${localFiles.size} " )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)//有网络连接时才执行任务
            // .setRequiresDeviceIdle(true) //设备休眠时执行任务
            //.setRequiresCharging(true)//设备充电时执行任务
            .build()

        val workerList = localFiles.map { file ->
            val data = TransferData(
                uploadPath, file.path, file.name, file.type, 1,
                System.currentTimeMillis()
            )
            transferDao.insertTransferData(data)
            OneTimeWorkRequestBuilder<TransferWorker>()
                //.setInitialDelay(5,TimeUnit.MINUTES)//延迟五分钟执行
                .setInputData(
                    Data.Builder()
                        .putString(CLOUD_LOCAL_PATH, file.path)
                        .putString(CLOUD_REMOTE_PATH, uploadPath)
                        .putInt(
                            CLOUD_TRANSFER_TYPE,
                            CLOUD_TRANSFER_TYPE_UPLOAD
                        )
                        .build()
                )//传入参数
                .setConstraints(constraints)//设置约束
                .build()

        }
        WorkManager.getInstance(context.applicationContext).enqueue(workerList)
        // info( "uploadFiles end " )
    }

    suspend fun downloadFiles(
        context: Context,
        localDir: String,
        remotePaths: List<String>
    ) {
        info("localDir  $localDir   localFiles： ${remotePaths.size} ")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)//有网络连接时才执行任务
            // .setRequiresDeviceIdle(true) //设备休眠时执行任务
            //.setRequiresCharging(true)//设备充电时执行任务
            .build()

        val remoteFiles = cloudDao.getCloudDataListByPathLiveData(remotePaths)

        val workerList = remoteFiles.map { file ->
            val data = TransferData(
                file.remotePath, localDir, file.displayName, file.fileType, 0,
                System.currentTimeMillis()
            )
            transferDao.insertTransferData(data)
            OneTimeWorkRequestBuilder<TransferWorker>()
                //.setInitialDelay(5,TimeUnit.MINUTES)//延迟五分钟执行
                .setInputData(
                    Data.Builder()
                        .putString(CLOUD_LOCAL_PATH, localDir)
                        .putString(CLOUD_REMOTE_PATH, file.remotePath)
                        .putInt(
                            CLOUD_TRANSFER_TYPE,
                            CLOUD_TRANSFER_TYPE_DOWNLOAD
                        )
                        .build()
                )//传入参数
                .setConstraints(constraints)//设置约束
                .build()

        }
        WorkManager.getInstance(context.applicationContext).enqueue(workerList)
        // info( "uploadFiles end " )
    }

    fun getTransferData(): LiveData<List<TransferData>> = transferDao.getTransferDataLiveData()

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: CloudLocalRepository? = null

        fun getInstance(
            transferDao: TransferDao,
            cloudDao: CloudDao
        ) =
            instance
                ?: synchronized(this) {
                    instance
                        ?: CloudLocalRepository(
                            transferDao,
                            cloudDao
                        )
                            .also { instance = it }
                }
    }
}