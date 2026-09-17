package io.github.yesterdaylz.kxqiandao.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Tab 状态工厂
 */
@Composable
fun rememberTabNavState(current: AppDestination): TabNavState {
    val checkIn = rememberNavBackStack(AppDestination.CheckIn)
    val ranking = rememberNavBackStack(AppDestination.Ranking)
    val knowledge = rememberNavBackStack(AppDestination.Knowledge)
    val mine = rememberNavBackStack(AppDestination.Mine)
    return remember(current) {
        TabNavState(
            current = current,
            stacks = mapOf(
                AppDestination.CheckIn to checkIn,
                AppDestination.Ranking to ranking,
                AppDestination.Knowledge to knowledge,
                AppDestination.Mine to mine,
            ),
        )
    }
}

/**
 * MainScreen 导航状态
 */
class TabNavState internal constructor(
    val current: AppDestination,
    private val stacks: Map<AppDestination, NavBackStack<NavKey>>,
) {

    // 当前 Tab 的回退栈
    val currentStack: NavBackStack<NavKey> get() = stacks.getValue(current)

    // 当前 Tab 的导航器
    val navigator: TabNavigator = TabNavigator(currentStack)
}
