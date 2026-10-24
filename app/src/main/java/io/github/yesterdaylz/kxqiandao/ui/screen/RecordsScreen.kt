package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.data.model.RecordItem
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.util.HeatmapBuilder
import io.github.yesterdaylz.kxqiandao.viewmodel.MainUiState
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel
import java.util.Locale

private const val PAGE_SIZE = 20

/**
 * 全部签到记录
 */
@Composable
fun RecordsScreen(
    padding: PaddingValues,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.fetchRecords() }
    val colors = KxTheme.colors

    val pageCount = ((uiState.records.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    var page by rememberSaveable { mutableStateOf(0) }
    val listState = rememberLazyListState()

    // 数据变少（刷新后）时把页码收回
    LaunchedEffect(pageCount) {
        if (page > pageCount - 1) page = pageCount - 1
    }
    // 翻页回到顶部
    LaunchedEffect(page) { listState.scrollToItem(0) }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
    ) {
        RecordsTopBar(
            onBack = onBack,
            onRefresh = { viewModel.fetchRecords(force = true) },
            refreshing = uiState.recordsLoading
        )

        if (uiState.recordsStale) {
            Text(
                text = "离线缓存，可能不是最新",
                fontSize = 12.sp,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }

        when {
            uiState.recordsLoading && uiState.records.isEmpty() -> Hint("加载中…")
            uiState.records.isEmpty() -> Hint("还没有签到记录")
            else -> {
                val pageItems = uiState.records.drop(page * PAGE_SIZE).take(PAGE_SIZE)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(pageItems) { record ->
                        RecordRow(record)
                        HorizontalDivider(
                            color = colors.divider,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                if (pageCount > 1) {
                    PageBar(page = page, pageCount = pageCount, onSelect = { page = it })
                }
            }
        }
    }
}

/** 返回箭头 + 标题 + 刷新 */
@Composable
private fun RecordsTopBar(onBack: () -> Unit, onRefresh: () -> Unit, refreshing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }
        Text(text = "签到记录", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onRefresh,
            enabled = !refreshing,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新签到记录",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 底部分页：‹ 1 … 5 6 7 … 12 › */
@Composable
private fun PageBar(page: Int, pageCount: Int, onSelect: (Int) -> Unit) {
    val colors = KxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageArrow(forward = false, enabled = page > 0) { onSelect(page - 1) }

        for (item in pageNumbers(page, pageCount)) {
            if (item == null) {
                Text(
                    text = "…",
                    fontSize = 12.sp,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                PageChip(
                    text = "${item + 1}",
                    selected = item == page,
                    onClick = { onSelect(item) }
                )
            }
        }

        PageArrow(forward = true, enabled = page < pageCount - 1) { onSelect(page + 1) }
    }
}

@Composable
private fun PageArrow(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = if (forward) {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            },
            contentDescription = if (forward) "下一页" else "上一页",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PageChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = KxTheme.colors
    Text(
        text = text,
        fontSize = 13.sp,
        color = if (selected) colors.onPrimary else colors.muted,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

/** 页码窗口，首页、末页、当前页前后各一页，中间 null 表示省略号 */
private fun pageNumbers(current: Int, count: Int): List<Int?> {
    if (count <= 7) return (0 until count).toList()

    val pages = sortedSetOf(0, count - 1)
    if (current - 1 > 0) pages.add(current - 1)
    if (current > 0) pages.add(current)
    if (current + 1 < count - 1) pages.add(current + 1)

    val result = mutableListOf<Int?>()
    var previous: Int? = null
    for (item in pages) {
        if (previous != null && item - previous > 1) result.add(null)
        result.add(item)
        previous = item
    }
    return result
}

@Composable
private fun RecordRow(record: RecordItem) {
    val colors = KxTheme.colors
    val duration = durationText(HeatmapBuilder.durationMinutes(record))
    val detail = listOfNotNull(
        record.userLocation?.takeIf { it.isNotBlank() },
        duration
    ).joinToString(" · ")
    val status = record.status.orEmpty()
    val statusColor = when (status) {
        "在线" -> colors.success
        "被迫下线" -> colors.danger
        else -> colors.muted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = record.start ?: "—", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = detail, fontSize = 12.sp, color = colors.muted)
        }
        Text(
            text = status,
            fontSize = 13.sp,
            color = statusColor
        )
    }
}

@Composable
private fun Hint(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text, fontSize = 13.sp, color = KxTheme.colors.muted)
    }
}

private fun durationText(minutes: Int): String = when {
    minutes <= 0 -> "不足 1 分钟"
    minutes < 60 -> "$minutes 分钟"
    else -> String.format(Locale.getDefault(), "%.1f 小时", minutes / 60f)
}
