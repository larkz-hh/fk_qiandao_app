package io.github.yesterdaylz.kxqiandao.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.model.SignData
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.NetworkUtil
import io.github.yesterdaylz.kxqiandao.util.errorText
import io.github.yesterdaylz.kxqiandao.util.userMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 小组件按钮实际动作
 */
object SignWidgetAction {

    suspend fun perform(context: Context) {
        val repo = SignRepository()
        val uid = repo.savedUid()
        if (uid.isBlank() || uid == "null") {
            toast(context, "请先在 App 里登录")
            SignWidgetProvider.renderAll(context)
            return
        }

        if (NetworkUtil.currentState(KexieApplication.instance) != NetworkUtil.NetState.Campus) {
            toast(context, "网络不可用，请连接科协网")
            SignWidgetProvider.renderAll(context)
            return
        }

        if (repo.ongoingStartTime() != null) {
            repo.signOut(uid)
                .onSuccess { res ->
                    when (res.code) {
                        0 -> {
                            res.data?.let { repo.saveSession(uid, it) }
                            repo.clearStartTime()
                            toast(context, "签退成功，辛苦了")
                        }
                        -202 -> {
                            repo.clearStartTime()
                            toast(context, "您还没有签到")
                        }
                        else -> toast(context, errorText(res.msg, "签退失败"))
                    }
                }
                .onFailure { toast(context, it.userMessage(Constants.NETWORK_ERROR)) }
        } else {
            repo.signIn(uid)
                .onSuccess { res ->
                    when (res.code) {
                        0 -> {
                            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Date())
                            repo.saveStartTime(now)
                            res.data?.let { repo.saveSession(uid, it) }
                            toast(context, "签到成功，美美上工")
                        }
                        -201 -> toast(context, "你已处于签到状态")
                        -205 -> toast(context, "操作太频繁，请稍后重试")
                        -206 -> toast(context, "该时段不允许签到")
                        else -> toast(context, errorText(res.msg, "签到失败"))
                    }
                }
                .onFailure { toast(context, it.userMessage(Constants.NETWORK_ERROR)) }
        }

        SignWidgetProvider.renderAll(context)
    }

    private fun toast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun SignRepository.saveSession(fallbackUid: String, data: SignData) {
        saveSession(
            uid = data.userId?.takeIf { it.isNotBlank() } ?: fallbackUid,
            uname = data.userName.orEmpty(),
            totalTime = data.totalTime.orEmpty(),
            week = data.week.orEmpty(),
        )
    }
}
