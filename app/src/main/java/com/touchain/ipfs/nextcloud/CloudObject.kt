package com.touchain.basic.module.nextcloud

import android.accounts.Account
import android.content.Context

object CloudObject {
    lateinit var account: Account
    lateinit var pathThumbnail: String
    lateinit var pathOrigin: String
    const val VCF_FILE = "temp.vcf"
    const val BACKUP_CONTACT_PATH = "/backup/contact/"
    const val BACKUP_PATH = "/backup/"
    const val ADDRESS = "touchain@127.0.0.1"
    const val PASSWORD = "touchain"
    const val USER = "touchain"
    const val URL = "http://127.0.0.1"
    const val SHORT_URL = "127.0.0.1"
    const val TYPE = "nextcloud"

    fun initAccount(context: Context) {
        /*  val accountMgr = AccountManager.get(context)
          account = Account(ADDRESS, TYPE)
          accountMgr.setPassword(account, PASSWORD)*/

        pathThumbnail = context.externalCacheDir?.path ?: context.cacheDir.path
        pathOrigin = "$pathThumbnail/origin"
    }
}