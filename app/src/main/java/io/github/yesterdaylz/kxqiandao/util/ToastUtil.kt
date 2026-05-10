package io.github.yesterdaylz.kxqiandao.util

import android.content.Context
import android.widget.Toast

fun String.showToast(context: Context, duration: Int= Toast.LENGTH_SHORT){
    Toast.makeText(context,this,duration).show()
}
fun Int.showToast(context: Context, duration: Int= Toast.LENGTH_SHORT){
    Toast.makeText(context,this,duration).show()
}

fun Int.showToast(context: Context, vararg args: Any?, duration: Int = Toast.LENGTH_SHORT) {
    val text = if (args.isEmpty()) context.getString(this) else context.getString(this, *args)
    Toast.makeText(context, text, duration).show()
}