package com.touchain.basic.module.nextcloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.resources.files.*
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation
import com.owncloud.android.lib.resources.shares.GetSharesForFileRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.OCShare.DEFAULT_PERMISSION
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.trashbin.ReadTrashbinFolderRemoteOperation
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.touchain.basic.common.NextCloudUtils
import com.touchain.basic.common.util.DidUtils
import com.touchain.basic.db.dao.CloudDao
import com.touchain.basic.db.entity.CloudData
import kotlinx.coroutines.Dispatchers
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class CloudRepository(private val cloudDao: CloudDao) : AnkoLogger {

    private lateinit var client: OwnCloudClient

    fun initClient(context: Context) {
        CloudObject.initAccount(context)

        val cloud = NextCloudUtils.getAccount()

        val serverUri =
            Uri.parse(CloudObject.URL)
        client =
            OwnCloudClientFactory.createOwnCloudClient(serverUri, context.applicationContext, true)
        client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
            cloud,
            cloud
        )

        client.userId = cloud
    }

    @WorkerThread
    fun searchState(context: Context): Array<Long> {
        initClient(context)

        try {
            var totalSize = 0L
            var usedSize = 0L
            var freeSize = 0L

            val result =
                GetUserInfoRemoteOperation().execute(client)
            info("GetUserInfoRemoteOperation  result:$result  data: ${result.data}")

            result.data.forEach {
                it as UserInfo
                //  info(" data: $it  ${it.quota} ")
                totalSize = it.quota.total
                usedSize = it.quota.used
                freeSize = it.quota.free
            }

            fetchRemoteFiles(FileUtils.PATH_SEPARATOR)
            //   info("GetServerInfoOperation  result:$refreshResult  data: ${refreshResult.data}")
            var videoSize = 0L
            var pictureSize = 0L
            var audioSize = 0L
            var documentSize = 0L
            //var otherSize = 0L
            val dataList = cloudDao.getCloudDataList()

            dataList.forEach {
                // info(" data: $it  ${it.remotePath}  ${it.mimeType}  ${it.length}")
                when (it.fileType) {
                    ITEM_TYPE_FILE_FOLDER -> {
                    }
                    ITEM_TYPE_FILE_AUDIO -> audioSize += it.size
                    ITEM_TYPE_FILE_PICTURE -> pictureSize += it.size
                    ITEM_TYPE_FILE_VIDEO -> videoSize += it.size
                    ITEM_TYPE_FILE_DOCUMENT -> documentSize += it.size
                    //TYPE_NEXT_CLOUD_OTHER -> otherSize += it.size
                }
            }
            val sum1 = pictureSize + videoSize
            val sum2 = sum1 + audioSize
            val sum3 = sum2 + documentSize

            info(" data: $videoSize  $pictureSize  $audioSize  $documentSize ")
            return arrayOf(
                totalSize, freeSize, pictureSize, sum1, sum2,
                sum3, usedSize
            )
        } catch (exception: Exception) {
            info("exception : $exception ")
            return emptyArray()
        }
    }

    @WorkerThread
    fun imageDownloadAndCompass(item: CloudData) {
        val rootPath = CloudObject.pathThumbnail
        val file = File("$rootPath${item.remotePath}")
        val downloadFileRemoteOperation = DownloadFileRemoteOperation(
            item.remotePath,
            rootPath
        )
        val downloadResult = downloadFileRemoteOperation.execute(client)
        info("downloadResult  result:$downloadResult  data: ${downloadResult.data}")
        if (!downloadResult.isSuccess) {
            return
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val bitmap1 = BitmapFactory.decodeFile(file.path, options) //此时返回bm为空

        //计算缩放比
        val imageHeight = 48 * 3
        val imageWidth = 48 * 3
        var be = (options.outHeight / imageHeight).toInt()
        if (be <= 0) be = 1
        options.inSampleSize = be
        //重新读入图片，注意这次要把options.inJustDecodeBounds 设为 false哦
        options.inJustDecodeBounds = false
        val bitmap2 = BitmapFactory.decodeFile(file.path, options) ?: return
        try {
            val out = FileOutputStream(file)
            if (bitmap2.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush()
                out.close()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @WorkerThread
    private fun fetchRemoteFiles(path: String) {
        //  info("fetchRemoteFiles  $path ")
        val dataList = cloudDao.getCloudDataByParent(path)
        val refreshOperation = ReadFolderRemoteOperation(path)
        val result = refreshOperation.execute(client)
        if (result.isSuccess) {
            result.data.forEach { item ->
                item as RemoteFile
                val needAdd = dataList.none {
                    it.etag == item.etag
                } && item.remotePath != path
                if (needAdd) {
                    val name = item.displayName()
                    val data = CloudData(
                        item.remoteId,
                        item.remotePath,
                        name,
                        item.fileType(),
                        item.mimeType,
                        item.length,
                        item.size,
                        item.etag,
                        item.isFavorite,
                        item.modifiedTimestamp,
                        path
                    )
                    //info("data  $data ")
                    if (item.mimeType.isPhoto()) {
                        imageDownloadAndCompass(data)
                    }
                    cloudDao.insertCloudData(data)
                    if (item.remotePath != path && item.mimeType.isDir()) {
                        fetchRemoteFiles(item.remotePath)
                    }
                }
            }
            val needDelete = dataList.filter { data ->
                result.data.none {
                    it as RemoteFile
                    it.remotePath == data.remotePath
                }
            }
            cloudDao.deleteCloudDatas(*needDelete.toTypedArray())
        }
    }

    fun getCloudDataByMenuType(@CloudMenuType type: Int): LiveData<List<CloudData>> {
        return when (type) {
            CLOUD_MENU_TYPE_VIDEO -> cloudDao.getCloudDataByTypeLiveData(
                ITEM_TYPE_FILE_VIDEO
            )
            CLOUD_MENU_TYPE_AUDIO -> cloudDao.getCloudDataByTypeLiveData(
                ITEM_TYPE_FILE_AUDIO
            )
            CLOUD_MENU_TYPE_DOCUMENT -> cloudDao.getCloudDataByTypeLiveData(
                ITEM_TYPE_FILE_DOCUMENT
            )
            CLOUD_MENU_TYPE_FOLDER -> cloudDao.getCloudDataByTypeLiveData(
                ITEM_TYPE_FILE_FOLDER
            )
            CLOUD_MENU_TYPE_MY_SPACE -> cloudDao.getCloudDataLiveData()
            CLOUD_MENU_TYPE_SEARCH -> cloudDao.getCloudDataLiveData()
            CLOUD_MENU_TYPE_PICTURE -> cloudDao.getCloudDataByTypeLiveData(
                ITEM_TYPE_FILE_PICTURE
            )
                .switchMap { pictureList ->
                    liveData(Dispatchers.IO) {
                        pictureList.filterNot {
                            val rootPath = CloudObject.pathThumbnail
                            val file = File("$rootPath${it.remotePath}")
                            file.exists()
                        }.forEach {
                            imageDownloadAndCompass(it)
                        }
                        emit(pictureList)
                    }
                }
            CLOUD_MENU_TYPE_FAVORITE -> cloudDao.getCloudDataFavoriteLiveData(true)
            CLOUD_MENU_TYPE_TRASH -> liveData(Dispatchers.IO) {
                val operation = ReadTrashbinFolderRemoteOperation(FileUtils.PATH_SEPARATOR)
                val result = operation.execute(client)
                info(" Trashbin  result: $result ")

                if (result.isSuccess) {
                    val list = result.data.map {
                        it as TrashbinFile
                        info(" TRASH file : $it  ${it.remotePath}  ${it.fullRemotePath}   ${it.originalLocation}")
                        CloudData(
                            it.remoteId,
                            it.remotePath,
                            it.displayName(),
                            it.fileType(),
                            it.mimeType,
                            it.fileLength,
                            it.fileLength,
                            it.originalLocation,
                            false,
                            it.deletionTimestamp
                        )
                    }
                    emit(list)
                } else {
                    emit(emptyList<CloudData>())
                }
            }
            else -> cloudDao.getCloudDataByTypeLiveData(ITEM_TYPE_FILE_VIDEO)
        }
    }

    @WorkerThread
    fun shareFiles(selectList: List<CloudData>): List<String> {
        try {
            val list = mutableListOf<String>()
            selectList.forEach { selectItem ->

                val operation: RemoteOperation =
                    GetSharesForFileRemoteOperation(selectItem.remotePath, false, false)
                val resultExists = operation.execute(client)

                var publicShareExists = false
                if (resultExists.isSuccess) {
                    var share: OCShare
                    for (i in resultExists.data.indices) {
                        share = resultExists.data[i] as OCShare
                        if (ShareType.PUBLIC_LINK == share.shareType) {
                            publicShareExists = true
                            list.add(share.shareLink)
                            break
                        }
                    }
                }

                if (publicShareExists) {
                    return@forEach
                }

                val remoteOperation = CreateShareRemoteOperation(
                    selectItem.remotePath,
                    ShareType.PUBLIC_LINK,
                    "",
                    false,
                    null,
                    DEFAULT_PERMISSION
                )
                remoteOperation.setGetShareDetails(true)
                val result = remoteOperation.execute(client)
                // info("shareFiles  ${selectItem.remotePath}  result:$result  data: ${result.data}")
                if (result.isSuccess) {
                    val share = result.data[0] as OCShare
                    //info("shareFiles  ${share.shareLink}  ${share.path}  data: ${share.isHideFileDownload}")
                    list.add(share.shareLink)
                }
            }
            return list
        } catch (exception: Exception) {
            info("moveFiles exception : $exception ")
        }
        return emptyList()
    }

    @WorkerThread
    fun share(remote: String): String {
        val op = CreateShareRemoteOperation(
            remote,
            ShareType.PUBLIC_LINK,
            "",
            false,
            null,
            DEFAULT_PERMISSION
        )

        val result = op.execute(client)
        if (result != null && result.data != null && result.data.size > 0) {
            val oc = result.data.get(0) as OCShare
            return oc.token
        } else {
            return ""
        }
    }

    @WorkerThread
    fun shareFile(remotePath: String): String {
        try {

            val operation: RemoteOperation =
                GetSharesForFileRemoteOperation(remotePath, false, false)
            val resultExists = operation.execute(client)

            if (resultExists.isSuccess) {
                var share: OCShare
                for (i in resultExists.data.indices) {
                    share = resultExists.data[i] as OCShare
                    if (ShareType.PUBLIC_LINK == share.shareType) {
                        return share.shareLink
                    }
                }
            }

            val remoteOperation = CreateShareRemoteOperation(
                remotePath,
                ShareType.PUBLIC_LINK,
                "",
                false,
                null,
                DEFAULT_PERMISSION
            )
            remoteOperation.setGetShareDetails(true)
            val result = remoteOperation.execute(client)
            // info("shareFiles  ${selectItem.remotePath}  result:$result  data: ${result.data}")
            if (result.isSuccess) {
                val share = result.data[0] as OCShare
                //info("shareFiles  ${share.shareLink}  ${share.path}  data: ${share.isHideFileDownload}")
                return share.shareLink
            }

        } catch (exception: Exception) {
            info("moveFiles exception : $exception ")
        }
        return "emptyList()"
    }

    @WorkerThread
    fun moveFiles(targetFolder: String, selectList: List<CloudData>) {
        try {
            selectList.forEach {
                val targetPath = "$targetFolder${it.displayName}"
                if (targetPath != it.remotePath) {
                    val remoteOperation = MoveFileRemoteOperation(
                        it.remotePath,
                        targetPath,
                        true
                    )
                    val result = remoteOperation.execute(client)
                    info("moveFiles  result:$result  data: ${result.data}")
                    if (result.isSuccess) {
                        cloudDao.deleteCloudDataByPath(it.remotePath)
                    }
                }
            }
        } catch (exception: Exception) {
            info("moveFiles exception : $exception ")
        }
    }

    @WorkerThread
    fun copyFiles(targetFolder: String, selectList: List<CloudData>) {
        try {
            selectList.forEach {
                val targetPath = "$targetFolder${it.displayName}"
                if (targetPath != it.remotePath) {
                    val remoteOperation = CopyFileRemoteOperation(
                        it.remotePath,
                        targetPath,
                        true
                    )
                    val result = remoteOperation.execute(client)
                    info("copyFiles  result:$result  data: ${result.data}")
                }
            }
        } catch (exception: Exception) {
            info("copyFiles exception : $exception ")
        }
    }

    @WorkerThread
    fun favoriteFiles(selectList: List<CloudData>) {
        try {
            selectList.forEach {
                val remoteOperation = ToggleFavoriteRemoteOperation(
                    true,
                    it.remotePath
                )
                val result = remoteOperation.execute(client)
                info("favoriteFiles  result:$result  data: ${result.data}")
            }
        } catch (exception: Exception) {
            info("favoriteFiles exception : $exception ")
        }
    }

    @WorkerThread
    fun favoriteFile(remotePath: String) {
        try {
            val remoteOperation = ToggleFavoriteRemoteOperation(
                true,
                remotePath
            )
            val result = remoteOperation.execute(client)
            info("favoriteFile  result:$result  data: ${result.data}")

        } catch (exception: Exception) {
            info("favoriteFiles exception : $exception ")
        }
    }

    @WorkerThread
    fun deleteFiles(selectList: List<CloudData>) {
        try {
            selectList.forEach {
                val remoteOperation = RemoveFileRemoteOperation(
                    it.remotePath
                )
                val result = remoteOperation.execute(client)
                info("deleteFiles  result:$result  data: ${result.data}")
                if (result.isSuccess) {
                    cloudDao.deleteCloudDataByPath(it.remotePath)
                }
            }
        } catch (exception: Exception) {
            info("deleteFiles exception : $exception ")
        }
    }

    @WorkerThread
    fun deleteFile(remotePath: String) {
        try {
            val remoteOperation = RemoveFileRemoteOperation(
                remotePath
            )
            val result = remoteOperation.execute(client)
            info("deleteFile  result:$result  data: ${result.data}")
            if (result.isSuccess) {
                cloudDao.deleteCloudDataByPath(remotePath)
            }

        } catch (exception: Exception) {
            info("deleteFile exception : $exception ")
        }
    }

    @WorkerThread
    fun downLoadFile(remotePath: String) {
        val rootPath = CloudObject.pathOrigin
        val file = File("$rootPath$remotePath")
        if (file.exists().not()) {
            val downloadFileRemoteOperation = DownloadFileRemoteOperation(
                remotePath,
                rootPath
            )
            val downloadResult = downloadFileRemoteOperation.execute(client)
            info("downLoadFile  result:$downloadResult  data: ${downloadResult.data}")
        }
    }

    @WorkerThread
    fun downloadFiles(
        remotePath: String, localPath: String,
        listener: OnDatatransferProgressListener
    ): Boolean {
        try {
            val remoteOperation = DownloadFileRemoteOperation(
                remotePath,
                localPath
            )
            info("uploadFile  localPath:$localPath  remote: $remotePath")
            remoteOperation.addDatatransferProgressListener(listener)
            val result = remoteOperation.execute(client)
            //info("uploadFile  result:$result  data: ${result.data}")
            return result.isSuccess
        } catch (exception: Exception) {
            info("uploadFile exception : $exception ")
        }
        return false
    }

    @WorkerThread
    fun uploadFile(
        remotePath: String, localPath: String,
        listener: OnDatatransferProgressListener
    ): Boolean {

        try {
            val file = File(localPath)
            val remoteOperation = ChunkedFileUploadRemoteOperation(
                localPath, "$remotePath${file.name}", getMimeType(localPath), null,
                (System.currentTimeMillis() / 1000).toString(), true
            )
            remoteOperation.addDataTransferProgressListener(listener)
            val result = remoteOperation.execute(client)
            return result.isSuccess
        } catch (exception: Exception) {
            info("uploadFile exception : $exception ")
        }
        return false
    }

    @WorkerThread
    fun checkFile(path: String): Boolean {
        try {
            val remoteOperation = ExistenceCheckRemoteOperation(
                path, false
            )
            val result = remoteOperation.execute(client)
            info("checkFile  result:$result  data: ${result.data}")
            return result.isSuccess

        } catch (exception: Exception) {
            info("deleteBackUpFile exception : $exception ")
        }
        return false
    }

    @WorkerThread
    fun deleteBackUpFile(path: String): Boolean {
        try {
            val remoteOperation = RemoveFileRemoteOperation(
                path
            )
            val result = remoteOperation.execute(client)
            info("deleteBackUpFile  result:$result  data: ${result.data}")
            return result.isSuccess

        } catch (exception: Exception) {
            info("deleteBackUpFile exception : $exception ")
        }
        return false
    }

    @WorkerThread
    fun downLoadBackUpFile(remotePath: String): String {
        val rootPath = CloudObject.pathOrigin
        val file = File("$rootPath$remotePath")
        return if (file.exists().not()) {
            val downloadFileRemoteOperation = DownloadFileRemoteOperation(
                remotePath,
                rootPath
            )
            val downloadResult = downloadFileRemoteOperation.execute(client)
            info("downLoadBackUpFile  result:$downloadResult  data: ${downloadResult.data}")
            if (downloadResult.isSuccess) {
                file.path
            } else {
                ""
            }
        } else {
            file.path
        }
    }

    @WorkerThread
    fun uploadBackUpFile(
        remotePath: String, localPath: String
    ): Boolean {
        try {
            val remoteOperation = UploadFileRemoteOperation(
                localPath, remotePath, getMimeType(localPath),
                (System.currentTimeMillis() / 1000).toString()
            )
            // info("uploadBackUpFile  localPath:$localPath  remote: $remotePath")
            //  remoteOperation.addDataTransferProgressListener(listener)
            val result = remoteOperation.execute(client)
            //   info("uploadBackUpFile  result:$result  data: ${result.data}")
            return result.isSuccess
        } catch (exception: Exception) {
            info("uploadBackUpFile exception : $exception ")
        }
        return false
    }

    @WorkerThread
    fun fetchBackUpFile(
        remotePath: String
    ): List<RemoteFile> {
        try {
            val remoteOperation = ReadFolderRemoteOperation(remotePath)
            //  remoteOperation.addDataTransferProgressListener(listener)
            val result = remoteOperation.execute(client)
            // info("fetchBackUpFile  result:$result  data: ${result.data}")
            if (result.isSuccess) {
                return result.data as List<RemoteFile>
            }
        } catch (exception: Exception) {
            info("fetchBackUpFile exception : $exception ")
        }
        return emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    fun getShareList(rootPath: String): List<OCShare> {
        try {
            val resultList = mutableListOf<OCShare>()
            val list = cloudDao.getCloudDataByType(ITEM_TYPE_FILE_FOLDER).map {
                it.remotePath
            } as MutableList

            list.add(rootPath)

            list.forEach {
                val operation: RemoteOperation = GetSharesForFileRemoteOperation(
                    it, true, true
                )
                val result = operation.execute(client)

                if (result.isSuccess) {
                    resultList.addAll(result.data as List<OCShare>)
                }
            }

            return resultList
        } catch (exception: Exception) {
            info("uploadFile exception : $exception ")
        }
        return emptyList()
    }

    @WorkerThread
    fun renameFile(
        oldName: String, oldRemotePath: String, newName: String,
        isFolder: Boolean
    ) {
        try {
            val remoteOperation = RenameFileRemoteOperation(
                oldName, oldRemotePath, newName, isFolder
            )
            val result = remoteOperation.execute(client)
            info("renameFile  result:$result  data: ${result.data}")

        } catch (exception: Exception) {
            info("renameFile exception : $exception ")
        }
    }

    @WorkerThread
    fun createFolder(remotePath: String, parent: String = ""): Boolean {
        try {
            val remoteOperation = CreateFolderRemoteOperation(
                remotePath, true
            )
            val result = remoteOperation.execute(client)
            info("createFolder  result:$result  data: ${result.data}")
            if (result.isSuccess && parent.isNotEmpty()) {
                fetchRemoteFiles(parent)
            }
            return result.isSuccess

        } catch (exception: Exception) {
            info("createFolder exception : $exception ")
        }
        return false
    }

    private fun getMimeType(filePath: String): String {
        val mmr = MediaMetadataRetriever()
        var mime = "text/plain"
        mime = try {
            mmr.setDataSource(filePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: IllegalStateException) {
            return mime
        } catch (e: IllegalArgumentException) {
            return mime
        } catch (e: RuntimeException) {
            return mime
        }

        return mime
    }

    fun fetchImage(imagePath: String) {
        val file = File("${CloudObject.pathOrigin}$imagePath")
        if (!file.exists()) {
            downLoadFile(imagePath)
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: CloudRepository? = null

        fun getInstance(cloudDao: CloudDao) =
            instance
                ?: synchronized(this) {
                    instance
                        ?: CloudRepository(
                            cloudDao
                        )
                            .also { instance = it }
                }
    }
}