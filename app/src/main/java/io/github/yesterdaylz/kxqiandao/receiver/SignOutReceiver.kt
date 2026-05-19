package io.github.yesterdaylz.kxqiandao.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.model.UserId
import io.github.yesterdaylz.kxqiandao.data.remote.RetrofitClient
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.service.AutoService
import io.github.yesterdaylz.kxqiandao.util.showToast
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SignOutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = KexieApplication.instance.preferencesManager
                val uid = prefs.uid.first()
                if (uid != "null" && uid.isNotEmpty()) {
                    val response = RetrofitClient.api.signOut(UserId(uid))
                    if (response.isSuccessful) {
                        val jsonStr = response.body()?.string()
                        if (jsonStr != null) {
                            val json = JSONObject(jsonStr)
                            if (json.getInt("code") == 0) {
                                val data = json.getJSONObject("data")
                                val totalTime = data.getString("totalTime")
                                val week = data.getString("week")
                                prefs.saveTotalTime(totalTime)
                                prefs.saveWeek(week)
                            }
                        }
                        prefs.saveStartTime("本次签到开始时间: null")
                        // 触发单例flow事件队列通知前台ui立即下线刷新
                        MainViewModel.alarmSignOutEvent.tryEmit(Unit)
                        withContext(Dispatchers.Main) {
                            Constants.SIGN_OUT_AUTO.showToast(context,Toast.LENGTH_LONG)

                        }
//                        Handler(Looper.getMainLooper()).post {
//                            Toast.makeText(context, "闹钟触发：已自动完成签退！", Toast.LENGTH_LONG).show()
//                        }
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
