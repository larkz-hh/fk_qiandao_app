package io.github.yesterdaylz.kxqiandao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.util.ToastBus
import io.github.yesterdaylz.kxqiandao.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    val topFiveList: List<TopFiveItem> = emptyList(),
)

class RankingViewModel : ViewModel() {

    private val repo = SignRepository()

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    /** 获取排名数据 */
    fun fetchTopFive() {
        viewModelScope.launch {
            repo.topFive()
                .onSuccess { list -> _uiState.update { it.copy(topFiveList = list) } }
                .onFailure {
                    it.printStackTrace()
                    ToastBus.send(it.userMessage("获取排行榜失败"))
                }
        }
    }
}
