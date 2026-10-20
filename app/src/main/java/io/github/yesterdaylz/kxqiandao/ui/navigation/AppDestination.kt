package io.github.yesterdaylz.kxqiandao.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用的顶级目的地
 *
 * 底部 Tab ：签到、排名、入口、我的。
 */
@Serializable
sealed interface AppDestination : NavKey, java.io.Serializable {

    @Serializable
    data object CheckIn : AppDestination

    @Serializable
    data object Ranking : AppDestination

    /** 入口页 */
    @Serializable
    data object Entries : AppDestination

    @Serializable
    data object Mine : AppDestination

    /** 知识库 */
    @Serializable
    data object Knowledge : AppDestination

    /** 校园跑 */
    @Serializable
    data object CampusRun : AppDestination

    /** 签到记录列表 */
    @Serializable
    data object Records : AppDestination
}
