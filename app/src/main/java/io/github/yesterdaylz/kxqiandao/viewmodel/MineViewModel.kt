package io.github.yesterdaylz.kxqiandao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.data.model.RecordItem
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.util.CacheEventBus
import io.github.yesterdaylz.kxqiandao.util.CheckInHeatmap
import io.github.yesterdaylz.kxqiandao.util.HeatmapBuilder
import io.github.yesterdaylz.kxqiandao.util.ToastBus
import io.github.yesterdaylz.kxqiandao.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val HEATMAP_MONTHS = 6
private const val MAX_HEATMAP_OFFSET = 40
private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

data class MineUiState(
    val heatmap: CheckInHeatmap? = null,
    val heatmapOffset: Int = 0,// 0 = 最近半年，-1 = 再往前半年
    val heatmapMinOffset: Int = 0,
    val heatmapLoading: Boolean = false,
    val records: List<RecordItem> = emptyList(),
    val recordsLoading: Boolean = false,
    val recordsStale: Boolean = false,
    val totalSignDays: Int = 0,
    val totalSignMinutes: Int = 0,
)

class MineViewModel : ViewModel() {

    private val repo = SignRepository()

    private val _uiState = MutableStateFlow(MineUiState())
    val uiState: StateFlow<MineUiState> = _uiState.asStateFlow()

    private var heatmapDays: Map<String, Int>? = null

    private var recordItems: List<RecordItem>? = null

    init {
        viewModelScope.launch {
            CacheEventBus.cleared.collect {
                heatmapDays = null
                recordItems = null
                _uiState.update {
                    it.copy(
                        heatmap = null,
                        heatmapOffset = 0,
                        heatmapMinOffset = 0,
                        records = emptyList(),
                        recordsStale = false,
                        totalSignDays = 0,
                        totalSignMinutes = 0,
                    )
                }
            }
        }
    }

    /** 加载热力图数据 */
    fun fetchHeatmap(force: Boolean = false) {
        if (_uiState.value.heatmapLoading) return
        if (!force && heatmapDays != null) return
        viewModelScope.launch {
            if (!force) {
                val cached = repo.heatmapCache()
                if (cached != null) {
                    applyHeatmapDays(cached.days)
                    if (System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) return@launch
                }
            }
            _uiState.update { it.copy(heatmapLoading = true) }
            val uid = repo.savedUid()
            if (uid == "null") {
                _uiState.update { it.copy(heatmapLoading = false) }
                return@launch
            }
            val records = repo.records(uid).getOrElse { error ->
                _uiState.update { it.copy(heatmapLoading = false) }
                ToastBus.send(error.userMessage("获取签到记录失败"))
                return@launch
            }
            recordItems = records
            val days = HeatmapBuilder.dailyMinutes(records)
            repo.saveHeatmapCache(days)
            repo.saveRecordsCache(records)
            applyHeatmapDays(days)
            if (force) ToastBus.send("签到记录已刷新")
        }
    }

    /** 加载签到记录 */
    fun fetchRecords(force: Boolean = false) {
        if (_uiState.value.recordsLoading) return
        val memory = recordItems
        if (!force && memory != null) {
            _uiState.update { it.copy(records = memory) }
            return
        }
        viewModelScope.launch {
            if (!force) {
                val cached = repo.recordsCache()
                if (cached != null) {
                    recordItems = cached.items
                    _uiState.update { it.copy(records = cached.items) }
                    if (System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) return@launch
                }
            }
            _uiState.update { it.copy(recordsLoading = true) }
            val uid = repo.savedUid()
            val records = if (uid == "null") {
                emptyList()
            } else {
                repo.records(uid).getOrElse { error ->
                    _uiState.update {
                        it.copy(recordsLoading = false, recordsStale = recordItems != null)
                    }
                    if (recordItems == null) {
                        ToastBus.send(error.userMessage("获取签到记录失败"))
                    }
                    return@launch
                }
            }
            recordItems = records
            repo.saveRecordsCache(records)
            val days = HeatmapBuilder.dailyMinutes(records)
            repo.saveHeatmapCache(days)
            applyHeatmapDays(days)
            _uiState.update {
                it.copy(records = records, recordsLoading = false, recordsStale = false)
            }
            if (force) ToastBus.send("签到记录已刷新")
        }
    }

    /** 前后翻半年 */
    fun shiftHeatmap(step: Int) {
        val state = _uiState.value
        val target = (state.heatmapOffset + step).coerceIn(state.heatmapMinOffset, 0)
        if (target == state.heatmapOffset) return
        rebuildHeatmap(target)
    }

    /** 套用按天统计的数据 */
    private fun applyHeatmapDays(days: Map<String, Int>) {
        heatmapDays = days
        val minOffset = days.keys.minOrNull()?.let(::minHeatmapOffset) ?: 0
        _uiState.update {
            it.copy(
                heatmapMinOffset = minOffset,
                heatmapOffset = it.heatmapOffset.coerceIn(minOffset, 0),
                heatmapLoading = false,
                totalSignDays = days.size,
                totalSignMinutes = days.values.sum(),
            )
        }
        rebuildHeatmap(_uiState.value.heatmapOffset)
    }

    /** 最早记录所在的半年 */
    private fun minHeatmapOffset(earliest: String): Int {
        var offset = 0
        while (offset > -MAX_HEATMAP_OFFSET && windowStart(offset) > earliest) {
            offset--
        }
        return offset
    }

    /** 某半年窗口的起始日 */
    private fun windowStart(offset: Int): String {
        val anchor = Calendar.getInstance().apply { add(Calendar.MONTH, offset * HEATMAP_MONTHS) }
        val start = (anchor.clone() as Calendar).apply { add(Calendar.MONTH, -HEATMAP_MONTHS) }
        start.add(Calendar.DAY_OF_YEAR, -((start.get(Calendar.DAY_OF_WEEK) + 5) % 7))
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(start.time)
    }

    /** 按偏移重建格子 */
    private fun rebuildHeatmap(offset: Int) {
        val days = heatmapDays ?: return
        val anchor = Calendar.getInstance().apply { add(Calendar.MONTH, offset * HEATMAP_MONTHS) }
        _uiState.update {
            it.copy(
                heatmap = HeatmapBuilder.build(days, HEATMAP_MONTHS, anchor),
                heatmapOffset = offset,
                heatmapLoading = false,
            )
        }
    }
}
