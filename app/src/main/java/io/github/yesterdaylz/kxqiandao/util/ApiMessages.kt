package io.github.yesterdaylz.kxqiandao.util

import io.github.yesterdaylz.kxqiandao.data.remote.ApiException

/** 服务端报错时优先用自己的提示 */
fun Throwable.userMessage(fallback: String): String =
    if (this is ApiException) message?.takeIf { it.isNotBlank() } ?: fallback else fallback

/** 响应里 msg  */
fun errorText(msg: String?, fallback: String): String =
    msg?.takeIf { it.isNotBlank() } ?: fallback
