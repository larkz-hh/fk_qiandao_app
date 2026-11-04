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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import io.github.yesterdaylz.kxqiandao.ui.component.RankingItem
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.viewmodel.RankingUiState
import kotlin.collections.lastIndex

/**
 * 排名页
 */
@Composable
fun RankingTab(
    padding: PaddingValues,
    uiState: RankingUiState,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onSelectGroup: (Boolean) -> Unit,
) {
    // 进页面先读缓存，再静默刷新
    LaunchedEffect(Unit) { onLoad() }
    val colors = KxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏆 排行榜",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRefresh,
                enabled = !uiState.loading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新排行榜",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RankingGroupChip(
                text = "新生",
                selected = !uiState.oldMan,
                onClick = { onSelectGroup(false) }
            )
            RankingGroupChip(
                text = "老生",
                selected = uiState.oldMan,
                onClick = { onSelectGroup(true) }
            )
        }

        if (uiState.stale) {
            Text(
                text = "离线缓存，可能不是最新",
                fontSize = 12.sp,
                color = colors.muted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            uiState.topFiveList.isEmpty() && uiState.loading -> RankingHint("加载中…")
            uiState.topFiveList.isEmpty() -> RankingHint("暂无排行数据")
            else -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp)
                ) {
                    itemsIndexed(uiState.topFiveList) { index, item ->
                        RankingItem(rank = index + 1, item = item)
                        if (index < uiState.topFiveList.lastIndex) {
                            HorizontalDivider(color = colors.divider, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

/** 新生 / 老生筛选 */
@Composable
private fun RankingGroupChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = KxTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceVariant,
            labelColor = colors.onSurface,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
        )
    )
}

/** 空状态 / 加载中 */
@Composable
private fun RankingHint(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, fontSize = 13.sp, color = KxTheme.colors.muted)
    }
}
