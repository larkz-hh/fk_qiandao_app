package io.github.yesterdaylz.kxqiandao.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 缓存清理通知 */
object CacheEventBus {
    private val _cleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cleared: SharedFlow<Unit> = _cleared.asSharedFlow()

    private val _webCleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 网页缓存清理 */
    val webCleared: SharedFlow<Unit> = _webCleared.asSharedFlow()

    fun notifyCleared() {
        _cleared.tryEmit(Unit)
    }

    fun notifyWebCleared() {
        _webCleared.tryEmit(Unit)
    }
}
