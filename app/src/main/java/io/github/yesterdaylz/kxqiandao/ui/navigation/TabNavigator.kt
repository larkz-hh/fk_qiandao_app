package io.github.yesterdaylz.kxqiandao.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 页面回退栈
 */
class TabNavigator(private val backStack: NavBackStack<NavKey>) {

    /** 进入下一个页面 */
    fun navigateTo(destination: AppDestination) {
        backStack.add(destination)
    }

    /** 返回上一层，栈底不做 */
    fun goBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    /** 当前 Tab 是否可返回 */
    val canGoBack: Boolean get() = backStack.size > 1
}

/**
 * 当前 Tab 的导航器
 */
val LocalTabNavigator = compositionLocalOf<TabNavigator> {
    error("LocalTabNavigator 只能在 MainScreen 的 NavDisplay 内部读取")
}
