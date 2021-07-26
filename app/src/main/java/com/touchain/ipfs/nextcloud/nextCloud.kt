package com.touchain.basic.module.nextcloud

import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile


fun String.nameIsVideo(): Boolean = this.endsWith("mp4") || this.endsWith("rm")
        || this.endsWith("vnd.ms-wpl")

fun String.nameIsPhoto(): Boolean = this.endsWith("jpg") || this.endsWith("gif")
        || this.endsWith("bmp")

fun String.nameIsAudio(): Boolean = this.endsWith("mp3") || this.endsWith("ogg")
        || this.endsWith("vnd.ms-wpl")

fun String.nameIsDocument(): Boolean = true


fun String.isDir(): Boolean = this == "DIR"

fun String.isVideo(): Boolean = this.startsWith("video")

fun String.isPhoto(): Boolean = this.startsWith("image")

fun String.isAudio(): Boolean = this.startsWith("audio") || this.endsWith("ogg")
        || this.contains("vnd.ms-wpl")

fun String.isDocument(): Boolean = this.startsWith("text") || this.endsWith("pdf")
        || this.endsWith("xml") || this.endsWith("excel")
        || this.endsWith("doc") || this.endsWith("document")
        || this.endsWith("powerpoint") || this.endsWith("html")

fun RemoteFile.displayName(): String {
    val array = remotePath.split("/")

    return if (mimeType.isDir()) {
        if (array.size < 3) remotePath.replace("/", "") else array[array.size - 2]
    } else {
        if (array.size < 2) {
            remotePath.replace("/", "")
        } else {
            array[array.size - 1]
        }
    }
}

fun TrashbinFile.displayName(): String {
    val array = originalLocation.split("/")
    return if (array.size < 2) {
        originalLocation.replace("/", "")
    } else {
        array[array.size - 1]
    }
}

fun OCShare.displayName(): String {
    val array = path.split("/")

    return if (isFolder) {
        if (array.size < 3) path.replace("/", "") else array[array.size - 2]
    } else {
        if (array.size < 2) {
            path.replace("/", "")
        } else {
            array[array.size - 1]
        }
    }
}

@ItemFileType
fun RemoteFile.fileType(): Int {
    return when {
        mimeType.isDir() -> ITEM_TYPE_FILE_FOLDER
        mimeType.isAudio() -> ITEM_TYPE_FILE_AUDIO
        mimeType.isPhoto() -> ITEM_TYPE_FILE_PICTURE
        mimeType.isVideo() -> ITEM_TYPE_FILE_VIDEO
        mimeType.isDocument() -> ITEM_TYPE_FILE_DOCUMENT
        else -> ITEM_TYPE_FILE_OTHER
    }
}

@ItemFileType
fun TrashbinFile.fileType(): Int {
    return when {
        mimeType.isDir() -> ITEM_TYPE_FILE_FOLDER
        mimeType.isAudio() -> ITEM_TYPE_FILE_AUDIO
        mimeType.isPhoto() -> ITEM_TYPE_FILE_PICTURE
        mimeType.isVideo() -> ITEM_TYPE_FILE_VIDEO
        mimeType.isDocument() -> ITEM_TYPE_FILE_DOCUMENT
        else -> ITEM_TYPE_FILE_OTHER
    }
}