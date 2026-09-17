package io.github.yesterdaylz.kxqiandao.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yesterdaylz.kxqiandao.ui.navigation.AppDestination

private data class BottomTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
)

/**
 *  Tab
 */
private val BottomTabs = listOf(
    BottomTab(AppDestination.CheckIn, "签到", Icons.Default.HowToReg),
    BottomTab(AppDestination.Ranking, "排名", Icons.Default.Leaderboard),
    BottomTab(AppDestination.Knowledge, "知识库", Icons.AutoMirrored.Filled.MenuBook),
    BottomTab(AppDestination.Mine, "我的", Icons.Default.Person),
)

/**
 * 底部导航栏
 */
@Composable
fun AppBottomBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = Color(0xFFF6F6F6)) {
        BottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab.destination,
                onClick = { onSelect(tab.destination) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}
