package io.github.yesterdaylz.kxqiandao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.util.CacheEventBus
import io.github.yesterdaylz.kxqiandao.util.ToastBus
import io.github.yesterdaylz.kxqiandao.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    val topFiveList: List<TopFiveItem> = emptyList(),
    val oldMan: Boolean = false,// false = 新生榜，true = 老生榜
    val loading: Boolean = false,
    val stale: Boolean = false,// 正在展示离线缓存
)

class RankingViewModel : ViewModel() {

    private val repo = SignRepository()

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val memory = mutableMapOf<Boolean, List<TopFiveItem>>()

    init {
        viewModelScope.launch {
            // 启动时先预载磁盘缓存
            for (oldMan in listOf(false, true)) {
                repo.rankingCache(oldMan)?.let { memory[oldMan] = it.items }
            }
            _uiState.update { it.copy(topFiveList = memory[it.oldMan] ?: emptyList()) }
            CacheEventBus.cleared.collect {
                memory.clear()
                _uiState.update { it.copy(topFiveList = emptyList(), stale = false) }
            }
        }
    }

    /** 新生 / 老生切换 */
    fun switchGroup(oldMan: Boolean) {
        if (_uiState.value.oldMan == oldMan) return
        _uiState.update {
            it.copy(
                oldMan = oldMan,
                topFiveList = memory[oldMan] ?: emptyList(),
                stale = false,
                loading = false,
            )
        }
        loadTopFive()
    }

    /** 进入排行榜，显示缓存，再静默刷新 */
    fun loadTopFive(force: Boolean = false) {
        val oldMan = _uiState.value.oldMan
        viewModelScope.launch {
            if (!force) {
                memory[oldMan]?.let { list ->
                    if (isCurrent(oldMan)) _uiState.update { it.copy(topFiveList = list) }
                }
                if (memory[oldMan] == null) {
                    repo.rankingCache(oldMan)?.let { cached ->
                        memory[oldMan] = cached.items
                        if (isCurrent(oldMan)) _uiState.update { it.copy(topFiveList = cached.items) }
                    }
                }
            }
            if (!isCurrent(oldMan)) return@launch
            fetch(oldMan, force)
        }
    }

    /** 手动刷新 */
    fun refresh() {
        loadTopFive(force = true)
    }

    /** 实际请求 */
    private suspend fun fetch(oldMan: Boolean, force: Boolean) {
        _uiState.update { it.copy(loading = true) }
        repo.topFive(oldMan)
            .onSuccess { list ->
                memory[oldMan] = list
                repo.saveRankingCache(oldMan, list)
                if (!isCurrent(oldMan)) return@onSuccess
                _uiState.update { it.copy(topFiveList = list, loading = false, stale = false) }
                if (force) ToastBus.send("排行榜已刷新")
            }
            .onFailure { error ->
                error.printStackTrace()
                if (!isCurrent(oldMan)) return@onFailure
                val hasCache = memory[oldMan] != null
                _uiState.update { it.copy(loading = false, stale = hasCache) }
                if (!hasCache) ToastBus.send(error.userMessage("获取排行榜失败"))
            }
    }

    private fun isCurrent(oldMan: Boolean): Boolean = _uiState.value.oldMan == oldMan
}
