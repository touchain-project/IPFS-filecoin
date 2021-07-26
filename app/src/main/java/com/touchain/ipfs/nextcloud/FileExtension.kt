package com.touchain.basic.module.nextcloud

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.touchain.basic.BuildConfig
import com.touchain.basic.R
import com.touchain.basic.common.view.showToast
import java.io.File
import java.util.*


fun Context.toTitle(@CloudMenuType type: Int): String {
    return when (type) {
        CLOUD_MENU_TYPE_VIDEO -> getString(R.string.main_storage_body_1_items_video_text)
        CLOUD_MENU_TYPE_AUDIO -> getString(R.string.main_storage_body_1_items_mp3_text)
        CLOUD_MENU_TYPE_DOCUMENT -> getString(R.string.main_storage_body_1_items_file_text)
        CLOUD_MENU_TYPE_PICTURE -> getString(R.string.main_storage_body_1_items_image_text)
        CLOUD_MENU_TYPE_FAVORITE -> getString(R.string.main_storage_body_2_my_collect_text)
        CLOUD_MENU_TYPE_TRASH -> getString(R.string.main_storage_body_2_trash_text)
        CLOUD_MENU_TYPE_MY_SPACE -> getString(R.string.upload_select_target_folder_main_item_my_space_text)
        CLOUD_MENU_TYPE_FOLDER -> getString(R.string.upload_select_target_folder_main_text)
        else -> ""
    }
}

fun Context.toSearchTitle(@CloudMenuType type: Int): String {
    return toTitle(type) + getString(R.string.public_search_text)
}

fun Context.openFile(filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        showToast("文件不存在!")
        return
    }
    /* 取得扩展名 */
    val end = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length).toLowerCase(
        Locale.getDefault()
    )
    /* 依扩展名的类型决定MimeType */
    var intent: Intent? = null
    if (end == "m4a" || end == "mp3" || end == "mid" || end == "xmf" || end == "ogg" || end == "wav" || end == "aac") {
        intent = generateVideoAudioIntent(
            filePath,
            DATA_TYPE_AUDIO
        )
    } else if (end == "3gp" || end == "mp4" || end == "mkv") {
        intent = generateVideoAudioIntent(
            filePath,
            DATA_TYPE_VIDEO
        )
    } else if (end == "jpg" || end == "gif" || end == "png" || end == "jpeg" || end == "bmp") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_IMAGE
        )
    } else if (end == "apk") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_APK
        )
    } else if (end == "html" || end == "htm") {
        intent = generateHtmlFileIntent(filePath)
    } else if (end == "ppt") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_PPT
        )
    } else if (end == "xls") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_EXCEL
        )
    } else if (end == "doc"||end=="docx") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_WORD
        )
    } else if (end == "pdf") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_PDF
        )
    } else if (end == "chm") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_CHM
        )
    } else if (end == "txt" || end == "json") {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_TXT
        )
    } else {
        intent = generateCommonIntent(
            filePath,
            DATA_TYPE_ALL
        )
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        showToast("手机找不到支持该格式的应用！")
    }
}

fun Context.openFileLocal(name: String) {
    val rootPath = CloudObject.pathOrigin
    val path = "$rootPath/$name"
    openFile(path)
}

fun Context.openFileRemote(remotePath: String) {
    val rootPath = CloudObject.pathOrigin
    val filePath = "$rootPath$remotePath"
    openFile(filePath)
}

private fun Context.getUri(intent: Intent, file: File): Uri {
    var uri: Uri? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //判断版本是否在7.0以上
        /*uri = FileProvider.getUriForFile(this, packageName.toString() + ".fileprovider",
            file)*/
        //添加这一句表示对目标应用临时授权该Uri所代表的文件
        uri = FileProvider.getUriForFile(
            Objects.requireNonNull(applicationContext),
            BuildConfig.APPLICATION_ID + ".provider", file
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        uri = Uri.fromFile(file)
    }
    return uri
}

private fun Context.generateCommonIntent(filePath: String, dataType: String): Intent {
    val intent = Intent()
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.action = Intent.ACTION_VIEW
    val file = File(filePath)
    val uri: Uri = getUri(intent, file)
    intent.setDataAndType(uri, dataType)
    return intent
}

private fun Context.generateHtmlFileIntent(filePath: String): Intent {
    val uri = Uri.parse(filePath)
        .buildUpon()
        .encodedAuthority("com.android.htmlfileprovider")
        .scheme("content")
        .encodedPath(filePath)
        .build()
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(
        uri,
        DATA_TYPE_HTML
    )
    return intent
}

private fun Context.generateVideoAudioIntent(filePath: String, dataType: String): Intent {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    intent.putExtra("oneshot", 0)
    intent.putExtra("configchange", 0)
    val file = File(filePath)
    intent.setDataAndType(getUri(intent, file), dataType)
    return intent
}

fun List<String>.toListString(): String {
    val sb = StringBuilder()
    forEachIndexed { index, content ->
        if (index < size - 1) {
            sb.append("$content*,*")
        } else {
            sb.append(content)
        }
    }
    return sb.toString()
}

fun String.toCombineList(): List<String> {
    val array = split("*,*")
    return array.toList()
}

private const val DATA_TYPE_ALL = "*/*" //未指定明确的文件类型，不能使用精确类型的工具打开，需要用户选择
private const val DATA_TYPE_APK = "application/vnd.android.package-archive"
private const val DATA_TYPE_VIDEO = "video/*"
private const val DATA_TYPE_AUDIO = "audio/*"
private const val DATA_TYPE_HTML = "text/html"
private const val DATA_TYPE_IMAGE = "image/*"
private const val DATA_TYPE_PPT = "application/vnd.ms-powerpoint"
private const val DATA_TYPE_EXCEL = "application/vnd.ms-excel"
private const val DATA_TYPE_WORD = "application/msword"
private const val DATA_TYPE_CHM = "application/x-chm"
private const val DATA_TYPE_TXT = "text/plain"
private const val DATA_TYPE_PDF = "application/pdf"