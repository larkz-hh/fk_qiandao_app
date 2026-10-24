package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.ui.component.RankingItem
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import kotlin.collections.lastIndex

/**
 * 排名页
 */
@Composable
fun RankingTab(padding: PaddingValues, topFiveList: List<TopFiveItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🏆 本周卷王榜 ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = KxTheme.colors.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                itemsIndexed(topFiveList) { index, item ->
                    RankingItem(rank = index + 1, item = item)
                    if (index < topFiveList.lastIndex) {
                        HorizontalDivider(color = KxTheme.colors.divider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}
