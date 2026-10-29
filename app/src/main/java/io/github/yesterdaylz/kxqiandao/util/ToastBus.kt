package io.github.yesterdaylz.kxqiandao.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 全局 Toast 出口 */
object ToastBus {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun send(message: String) {
        _messages.tryEmit(message)
    }
}
