package io.github.yesterdaylz.kxqiandao.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * 应用内更新，系统 DownloadManager 下 APK
 */
object ApkUpdateManager {

    private const val APK_MIME = "application/vnd.android.package-archive"
    private var downloadingId = -1L

    fun start(context: Context, url: String, version: String) {
        if (!canInstall(context)) {
            "请先允许安装未知应用，再回来点一次下载".showToast(context, Toast.LENGTH_LONG)
            openInstallPermission(context)
            return
        }
        if (downloadingId != -1L) {
            "更新包正在下载中…".showToast(context)
            return
        }

        val manager = context.getSystemService(DownloadManager::class.java) ?: run {
            "下载服务不可用".showToast(context)
            return
        }

        val request = DownloadManager.Request(url.toUri())
            .setTitle("三院科协签到 $version")
            .setDescription("正在下载更新包")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "kx-qiandao-${version.removePrefix("v")}.apk"
            )

        val id = runCatching { manager.enqueue(request) }.getOrNull()
        if (id == null) {
            "开始下载失败，试试用浏览器打开".showToast(context, Toast.LENGTH_LONG)
            return
        }
        downloadingId = id
        "开始下载更新包…".showToast(context)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != downloadingId) return
                runCatching { receiverContext.unregisterReceiver(this) }
                downloadingId = -1L
                install(receiverContext, manager, finishedId)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /** 下载完成后拉起系统安装器 */
    private fun install(context: Context, manager: DownloadManager, id: Long) {
        val uri = manager.getUriForDownloadedFile(id)
        if (uri == null) {
            "更新包下载失败".showToast(context, Toast.LENGTH_LONG)
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, APK_MIME)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            "更新包已下载到 Downloads，请在通知栏点击安装".showToast(context, Toast.LENGTH_LONG)
        }
    }

    private fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.packageManager.canRequestPackageInstalls()

    private fun openInstallPermission(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${context.packageName}".toUri()
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
