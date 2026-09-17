package io.github.yesterdaylz.kxqiandao.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 链接路由
 */
object UrlRouter {

    private val INTERNAL_HOSTS = listOf("feishu.cn", "larksuite.com", "feishu.net")

    /** host 是否属于站内域名 */
    fun isInternalHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return INTERNAL_HOSTS.any { h == it || h.endsWith(".$it") }
    }

    /** 系统打开 */
    fun openExternally(context: Context, uri: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            "没有能打开该链接的应用".showToast(context)
        }
    }
}
