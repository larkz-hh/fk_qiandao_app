package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.ui.navigation.AppDestination
import io.github.yesterdaylz.kxqiandao.ui.navigation.LocalTabNavigator
import io.github.yesterdaylz.kxqiandao.ui.theme.Blue1
import io.github.yesterdaylz.kxqiandao.ui.theme.GrayOffline
import io.github.yesterdaylz.kxqiandao.util.CheckInHeatmap
import io.github.yesterdaylz.kxqiandao.viewmodel.MainUiState
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel
import java.util.Locale

/**
 * 我的页
 */
@Composable
fun MineTab(
    padding: PaddingValues,
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    LaunchedEffect(Unit) { viewModel.fetchHeatmap() }
    val navigator = LocalTabNavigator.current

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        ProfileHeader(
            uid = uiState.uid,
            uname = uiState.uname,
            totalDays = uiState.totalSignDays,
            totalMinutes = uiState.totalSignMinutes
        )

        Spacer(modifier = Modifier.height(20.dp))
        HeatmapCard(
            heatmap = uiState.heatmap,
            loading = uiState.heatmapLoading,
            canGoPrev = uiState.heatmapOffset > uiState.heatmapMinOffset,
            canGoNext = uiState.heatmapOffset < 0,
            onPrev = { viewModel.shiftHeatmap(-1) },
            onNext = { viewModel.shiftHeatmap(1) },
            onRefresh = { viewModel.fetchHeatmap(force = true) }
        )

        Spacer(modifier = Modifier.height(6.dp))
        RecordsEntry(onClick = { navigator.navigateTo(AppDestination.Records) })

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "更多功能施工中，敬请期待", fontSize = 13.sp, color = GrayOffline)
    }
}

/** 进入完整签到记录 */
@Composable
private fun RecordsEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "查看全部签到记录", fontSize = 14.sp, color = Blue1)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Blue1,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 姓名 / 学号 / 累计签到 */
@Composable
private fun ProfileHeader(uid: String, uname: String, totalDays: Int, totalMinutes: Int) {
    val name = uname.takeUnless { it.isBlank() || it == "null" } ?: "未登录"
    val id = uid.takeUnless { it.isBlank() || it == "null" } ?: "—"
    Column {
        Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "学号 $id", fontSize = 13.sp, color = GrayOffline)
        if (totalDays > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "累计 $totalDays 天 · ${formatHours(totalMinutes)} 小时",
                fontSize = 13.sp,
                color = Blue1
            )
        }
    }
}

/** 热力图，右上角是刷新和翻半年 */
@Composable
private fun HeatmapCard(
    heatmap: CheckInHeatmap?,
    loading: Boolean,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "签到记录", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onRefresh,
                    enabled = !loading,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新签到记录",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onPrev,
                    enabled = canGoPrev,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "往前半年",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "往后半年",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            when (heatmap) {
                null if loading -> Text(
                    text = "加载中…",
                    fontSize = 13.sp,
                    color = GrayOffline
                )
                null -> Text(
                    text = "暂无签到记录",
                    fontSize = 13.sp,
                    color = GrayOffline
                )
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${heatmap.startDate} ~ ${heatmap.endDate}",
                            fontSize = 11.sp,
                            color = GrayOffline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "共 ${formatHours(heatmap.totalMinutes)} 小时 · ${heatmap.activeDays} 天",
                            fontSize = 11.sp,
                            color = GrayOffline,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HeatmapGrid(heatmap)
                    Spacer(modifier = Modifier.height(8.dp))
                    HeatmapLegend()
                }
            }
        }
    }
}

@Composable
private fun HeatmapGrid(heatmap: CheckInHeatmap) {
    Canvas(modifier = Modifier.fillMaxWidth().height(104.dp)) {
        val gap = 3.dp.toPx()
        val columns = heatmap.weeks.size
        if (columns == 0) return@Canvas

        val cell = ((size.width - gap * (columns - 1)) / columns)
            .coerceIn(3.dp.toPx(), 12.dp.toPx())
        val gridWidth = cell * columns + gap * (columns - 1)
        val gridHeight = cell * 7 + gap * 6
        val left = (size.width - gridWidth) / 2
        val top = (size.height - gridHeight) / 2
        val radius = CornerRadius(2.dp.toPx())

        heatmap.weeks.forEachIndexed { columnIndex, week ->
            week.forEachIndexed { rowIndex, day ->
                if (day != null) {
                    drawRoundRect(
                        color = heatColor(day.level),
                        topLeft = Offset(
                            left + columnIndex * (cell + gap),
                            top + rowIndex * (cell + gap)
                        ),
                        size = Size(cell, cell),
                        cornerRadius = radius
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "少", fontSize = 11.sp, color = GrayOffline)
        Spacer(modifier = Modifier.width(4.dp))
        for (level in 0..4) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawRoundRect(
                    color = heatColor(level),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
        }
        Spacer(modifier = Modifier.width(1.dp))
        Text(text = "多", fontSize = 11.sp, color = GrayOffline)
    }
}

private fun heatColor(level: Int): Color = when (level) {
    1 -> Color(0xFF9BE9A8)
    2 -> Color(0xFF40C463)
    3 -> Color(0xFF30A14E)
    4 -> Color(0xFF216E39)
    else -> Color(0xFFEBEDF0)
}

private fun formatHours(minutes: Int): String =
    String.format(Locale.getDefault(), "%.1f", minutes / 60f)
