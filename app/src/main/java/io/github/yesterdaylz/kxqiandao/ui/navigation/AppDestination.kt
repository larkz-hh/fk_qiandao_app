package io.github.yesterdaylz.kxqiandao.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用的顶级目的地
 */
@Serializable
sealed interface AppDestination : NavKey, java.io.Serializable {

    @Serializable
    data object CheckIn : AppDestination

    @Serializable
    data object Ranking : AppDestination

    @Serializable
    data object Knowledge : AppDestination

    @Serializable
    data object Mine : AppDestination
}
