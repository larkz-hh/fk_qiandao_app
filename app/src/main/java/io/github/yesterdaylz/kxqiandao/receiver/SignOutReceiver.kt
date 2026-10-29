package io.github.yesterdaylz.kxqiandao.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.service.AutoService
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.showToast
import io.github.yesterdaylz.kxqiandao.viewmodel.SignViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignOutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val pendingResult = goAsync()
        val repo = SignRepository()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = repo.savedUid()
                if (uid != "null" && uid.isNotEmpty()) {
                    val body = repo.signOut(uid).getOrNull()
                    if (body != null) {
                        if (body.code == 0) {
                            body.data?.let { data ->
                                repo.saveSession(
                                    uid = data.userId.orEmpty(),
                                    uname = data.userName.orEmpty(),
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                )
                            }
                        }

                        repo.clearStartTime()
                        // 触发单例 flow 事件队列通知前台ui立即下线刷新
                        SignViewModel.alarmSignOutEvent.tryEmit(Unit)
                        withContext(Dispatchers.Main) {
                            Constants.SIGN_OUT_AUTO.showToast(context, Toast.LENGTH_LONG)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                context.stopService(Intent(context, AutoService::class.java))
                pendingResult.finish()
            }
        }
    }
}
