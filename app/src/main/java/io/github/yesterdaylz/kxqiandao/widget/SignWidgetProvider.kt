package io.github.yesterdaylz.kxqiandao.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.MainActivity
import io.github.yesterdaylz.kxqiandao.R
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 桌面小组件
 */
class SignWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    /// 拖动改变大小时重新渲染
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        private const val REQUEST_OPEN = 100
        private const val REQUEST_ACTION = 101

        // 低于此高度用紧凑布局
        private const val COMPACT_HEIGHT_DP = 100

        suspend fun renderAll(context: Context) {
            val repo = SignRepository()
            val uid = repo.savedUid()
            val name = runCatching {
                KexieApplication.instance.preferencesManager.uname.first()
            }.getOrNull().orEmpty()
            val startTime = repo.ongoingStartTime()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SignWidgetProvider::class.java))
            if (ids.isEmpty()) return

            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val compact = isCompact(options)
                manager.updateAppWidget(id, buildViews(context, uid, name, startTime, compact))
            }
        }

        private fun isCompact(options: Bundle): Boolean {
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            return height in 1 until COMPACT_HEIGHT_DP
        }

        fun buildViews(
            context: Context,
            uid: String,
            name: String,
            startTime: String?,
            compact: Boolean,
        ): RemoteViews {
            val layout = if (compact) R.layout.widget_sign_compact else R.layout.widget_sign
            val views = RemoteViews(context.packageName, layout)
            val loggedIn = uid.isNotBlank() && uid != "null"

            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))

            if (!loggedIn) {
                views.setTextViewText(R.id.widget_name, "未登录")
                views.setTextViewText(
                    R.id.widget_status,
                    context.getString(R.string.sign_widget_not_login)
                )
                views.setTextViewText(
                    R.id.widget_action,
                    context.getString(R.string.sign_widget_sign_in)
                )
                if (!compact) views.setViewVisibility(R.id.widget_timer, View.GONE)
                views.setOnClickPendingIntent(R.id.widget_action, openAppIntent(context))
                applyButtonStyle(context, views, signOut = false)
                return views
            }

            views.setTextViewText(
                R.id.widget_name,
                if (name.isBlank() || name == "null") uid else name
            )
            views.setOnClickPendingIntent(R.id.widget_action, actionIntent(context))

            val online = startTime != null
            views.setTextViewText(
                R.id.widget_action,
                context.getString(
                    if (online) R.string.sign_widget_sign_out else R.string.sign_widget_sign_in
                )
            )
            applyButtonStyle(context, views, signOut = online)

            if (online) {
                val base = chronometerBase(startTime.orEmpty())
                views.setTextViewText(
                    R.id.widget_status,
                    if (base > 0L) "在线 · ${elapsedText(startTime.orEmpty())}" else "在线"
                )
                if (!compact) {
                    if (base > 0L) {
                        views.setChronometer(
                            R.id.widget_timer,
                            base,
                            context.getString(R.string.sign_widget_timer_format),
                            true
                        )
                        views.setViewVisibility(R.id.widget_timer, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.widget_timer, View.GONE)
                    }
                }
            } else {
                views.setTextViewText(
                    R.id.widget_status,
                    context.getString(R.string.sign_widget_not_online)
                )
                if (!compact) views.setViewVisibility(R.id.widget_timer, View.GONE)
            }
            return views
        }

        private fun applyButtonStyle(context: Context, views: RemoteViews, signOut: Boolean) {
            views.setInt(
                R.id.widget_action,
                "setBackgroundResource",
                if (signOut) R.drawable.widget_btn_sign_out else R.drawable.widget_btn_sign_in
            )
            views.setTextColor(
                R.id.widget_action,
                ContextCompat.getColor(
                    context,
                    if (signOut) R.color.widget_btn_sign_out_text else R.color.widget_btn_sign_in_text
                )
            )
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun actionIntent(context: Context): PendingIntent {
            val intent = Intent(context, SignWidgetActionReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_ACTION,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /// yyyy-MM-dd HH:mm:ss 转成 Chronometer 的 base（elapsedRealtime 基准）
        private fun chronometerBase(startTime: String): Long {
            val parsed = parseStart(startTime) ?: return 0L
            val elapsed = (System.currentTimeMillis() - parsed).coerceAtLeast(0L)
            return SystemClock.elapsedRealtime() - elapsed
        }

        private fun elapsedText(startTime: String): String {
            val parsed = parseStart(startTime) ?: return "--:--"
            val minutes = ((System.currentTimeMillis() - parsed) / 60_000L).coerceAtLeast(0L)
            return if (minutes < 60) {
                "${minutes}分钟"
            } else {
                "${minutes / 60}小时${minutes % 60}分"
            }
        }

        private fun parseStart(startTime: String): Long? = runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(startTime)?.time
        }.getOrNull()
    }
}

/// 刷新小组件，App 内状态变化后调用
fun refreshSignWidget() {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching { SignWidgetProvider.renderAll(KexieApplication.instance) }
    }
}
