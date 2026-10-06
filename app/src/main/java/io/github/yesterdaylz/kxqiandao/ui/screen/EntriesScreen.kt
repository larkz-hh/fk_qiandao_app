package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.ui.navigation.AppDestination
import io.github.yesterdaylz.kxqiandao.ui.navigation.LocalTabNavigator
import io.github.yesterdaylz.kxqiandao.ui.theme.Blue1
import io.github.yesterdaylz.kxqiandao.ui.theme.Blue2
import io.github.yesterdaylz.kxqiandao.ui.theme.DividerGray
import io.github.yesterdaylz.kxqiandao.ui.theme.GrayOffline

/**
 * 入口项
 */
private data class AppEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: AppDestination,
)

/**
 * 入口页的入口列表
 */
private val AppEntries = listOf(
    AppEntry(
        title = "知识库",
        subtitle = "科协知识库文档与资料",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        destination = AppDestination.Knowledge,
    ),
)

/**
 * 入口页
 */
@Composable
fun EntriesTab(padding: PaddingValues) {
    val navigator = LocalTabNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "全部入口", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppEntries.forEach { entry ->
                EntryCard(entry = entry, onClick = { navigator.navigateTo(entry.destination) })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** 单个入口卡片 */
@Composable
private fun EntryCard(entry: AppEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Blue2.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Blue1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = entry.subtitle, fontSize = 12.sp, color = GrayOffline)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DividerGray
            )
        }
    }
}
