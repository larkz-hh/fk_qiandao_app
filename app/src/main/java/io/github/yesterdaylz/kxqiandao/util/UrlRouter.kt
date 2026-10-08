package io.github.yesterdaylz.kxqiandao.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 链接路由
 */
object UrlRouter {

    private val FEISHU_HOSTS = listOf("feishu.cn", "larksuite.com", "feishu.net")

    /** host 是否属于站内域名 */
    fun isInternalHost(host: String?, internalHosts: List<String>): Boolean {
        val h = host?.lowercase() ?: return false
        return internalHosts.any { h == it || h.endsWith(".$it") }
    }

    /** 知识库站内域名判定 */
    fun isKnowledgeHost(host: String?): Boolean = isInternalHost(host, FEISHU_HOSTS)

    /** 系统打开 */
    fun openExternally(context: Context, uri: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            "没有能打开该链接的应用".showToast(context)
        }
    }
}
