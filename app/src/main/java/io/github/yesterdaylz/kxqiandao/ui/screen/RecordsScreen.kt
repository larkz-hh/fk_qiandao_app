package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.data.model.RecordItem
import io.github.yesterdaylz.kxqiandao.ui.theme.DividerGray
import io.github.yesterdaylz.kxqiandao.ui.theme.GrayOffline
import io.github.yesterdaylz.kxqiandao.ui.theme.GreenOnline
import io.github.yesterdaylz.kxqiandao.util.HeatmapBuilder
import io.github.yesterdaylz.kxqiandao.viewmodel.MainUiState
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel
import java.util.Locale

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

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
    ) {
        RecordsTopBar(onBack = onBack)

        if (uiState.recordsStale) {
            Text(
                text = "离线缓存，可能不是最新",
                fontSize = 12.sp,
                color = GrayOffline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }

        when {
            uiState.recordsLoading && uiState.records.isEmpty() -> Hint("加载中…")
            uiState.records.isEmpty() -> Hint("还没有签到记录")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.records) { record ->
                    RecordRow(record)
                    HorizontalDivider(
                        color = DividerGray,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

/** 返回箭头 + 标题 */
@Composable
private fun RecordsTopBar(onBack: () -> Unit) {
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
    }
}

@Composable
private fun RecordRow(record: RecordItem) {
    val duration = durationText(HeatmapBuilder.durationMinutes(record))
    val detail = listOfNotNull(
        record.userLocation?.takeIf { it.isNotBlank() },
        duration
    ).joinToString(" · ")
    val status = record.status.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = record.start ?: "—", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = detail, fontSize = 12.sp, color = GrayOffline)
        }
        Text(
            text = status,
            fontSize = 13.sp,
            color = if (status == "在线") GreenOnline else GrayOffline
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
        Text(text = text, fontSize = 13.sp, color = GrayOffline)
    }
}

private fun durationText(minutes: Int): String = when {
    minutes <= 0 -> "不足 1 分钟"
    minutes < 60 -> "$minutes 分钟"
    else -> String.format(Locale.getDefault(), "%.1f 小时", minutes / 60f)
}
