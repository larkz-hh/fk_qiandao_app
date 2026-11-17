package io.github.yesterdaylz.kxqiandao.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SignWidgetAction.perform(context)
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }
}
