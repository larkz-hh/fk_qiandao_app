package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

/** 排行榜缓存，榜单、拉取时间 */
@JsonClass(generateAdapter = true)
data class RankingCache(
    val fetchedAt: Long = 0,
    val items: List<TopFiveItem> = emptyList()
)
