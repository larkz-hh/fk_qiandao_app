package io.github.yesterdaylz.kxqiandao.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme

@Composable
fun RankingItem(rank: Int, item: TopFiveItem) {
    val colors = KxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //排名序号
        Text(
            text = "$rank",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) Color(0xFFFF9800) else colors.muted,
            modifier = Modifier.width(36.dp)
        )

        //成员信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.userName ?: "未知姓名",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.userDept ?: "未知部门",
                fontSize = 12.sp,
                color = colors.muted
            )
        }
        // 签到总时长
        Text(
            text = item.totalTime ?: "0",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
    }
}