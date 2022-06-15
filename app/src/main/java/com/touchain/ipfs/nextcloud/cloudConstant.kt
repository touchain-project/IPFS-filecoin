package com.touchain.basic.module.nextcloud

import androidx.annotation.IntDef
import androidx.annotation.StringDef

const val DATABASE_NAME = "touchain-basic-db"

const val CLOUD_LOCAL_PATH = "local path"
const val CLOUD_REMOTE_PATH = "remote path"
const val CLOUD_TRANSFER_TYPE = "transfer type"
const val CLOUD_WORKER_PROGRESS = "cloud progress"

const val ITEM_CHECK = 101
const val ITEM_UNCHECK = 102
const val NO_STATE = 103

@IntDef(
    ITEM_CHECK,
    ITEM_UNCHECK,
    NO_STATE
)
annotation class CheckState

@IntDef(
    ITEM_TYPE_FILE_VIDEO,
    ITEM_TYPE_FILE_AUDIO,
    ITEM_TYPE_FILE_DOCUMENT,
    ITEM_TYPE_FILE_PICTURE,
    ITEM_TYPE_FILE_TITLE,
    ITEM_TYPE_FILE_FOLDER,
    ITEM_TYPE_FILE_TITLE_EMPTY
)
annotation class ItemFileType

const val ITEM_TYPE_FILE_VIDEO = 101
const val ITEM_TYPE_FILE_AUDIO = 102
const val ITEM_TYPE_FILE_DOCUMENT = 103
const val ITEM_TYPE_FILE_PICTURE = 104
const val ITEM_TYPE_FILE_TITLE = 105
const val ITEM_TYPE_FILE_FOLDER = 106
const val ITEM_TYPE_FILE_OTHER = 107
const val ITEM_TYPE_FILE_PICTURE_COMMON = 108
const val ITEM_TYPE_FILE_TITLE_EMPTY = 109

@IntDef(
    CLOUD_MENU_TYPE_VIDEO,
    CLOUD_MENU_TYPE_AUDIO,
    CLOUD_MENU_TYPE_DOCUMENT,
    CLOUD_MENU_TYPE_PICTURE,
    CLOUD_MENU_TYPE_FAVORITE,
    CLOUD_MENU_TYPE_TRASH,
    CLOUD_MENU_TYPE_SHARE,
    CLOUD_MENU_TYPE_TRANSPORT,
    CLOUD_MENU_TYPE_FOLDER,
    CLOUD_MENU_TYPE_MY_SPACE,
    CLOUD_MENU_TYPE_SEARCH
)
annotation class CloudMenuType

const val CLOUD_MENU_TYPE_VIDEO = 201
const val CLOUD_MENU_TYPE_AUDIO = 202
const val CLOUD_MENU_TYPE_DOCUMENT = 203
const val CLOUD_MENU_TYPE_PICTURE = 204
const val CLOUD_MENU_TYPE_FAVORITE = 205
const val CLOUD_MENU_TYPE_TRASH = 206
const val CLOUD_MENU_TYPE_SHARE = 207
const val CLOUD_MENU_TYPE_TRANSPORT = 208
const val CLOUD_MENU_TYPE_FOLDER = 209
const val CLOUD_MENU_TYPE_MY_SPACE = 210
const val CLOUD_MENU_TYPE_SEARCH = 211

@StringDef(
    CLOUD_TARGET_FOLDER_MOVE,
    CLOUD_TARGET_FOLDER_COPY,
    CLOUD_TARGET_FOLDER_UPLOAD
)
annotation class CloudTargetType

const val CLOUD_TARGET_FOLDER_MOVE = "move"
const val CLOUD_TARGET_FOLDER_COPY = "copy"
const val CLOUD_TARGET_FOLDER_UPLOAD = "upload"

@IntDef(
    CLOUD_TRANSFER_TYPE_UPLOAD,
    CLOUD_TRANSFER_TYPE_DOWNLOAD,
    CLOUD_TRANSFER_TYPE_BACKUP,
    CLOUD_TRANSFER_TYPE_DONE
)
annotation class CloudTransferType

const val CLOUD_TRANSFER_TYPE_UPLOAD = 1
const val CLOUD_TRANSFER_TYPE_DOWNLOAD = 0
const val CLOUD_TRANSFER_TYPE_BACKUP = 2
const val CLOUD_TRANSFER_TYPE_DONE = 3


const val CLOUD_PHONE_FLOW = "phone_flow"
const val CLOUD_PHONE_FLOW_TRANSFER = "phone_flow_transfer"
const val CLOUD_PHONE_GALLERY_BACK_UP = "gallery_back_up"
const val CLOUD_PHONE_VIDEO_BACK_UP = "video_back_up"
