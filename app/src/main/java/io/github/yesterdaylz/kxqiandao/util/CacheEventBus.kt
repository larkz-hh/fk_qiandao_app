package io.github.yesterdaylz.kxqiandao.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 缓存清理通知 */
object CacheEventBus {
    private val _cleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cleared: SharedFlow<Unit> = _cleared.asSharedFlow()

    fun notifyCleared() {
        _cleared.tryEmit(Unit)
    }
}
