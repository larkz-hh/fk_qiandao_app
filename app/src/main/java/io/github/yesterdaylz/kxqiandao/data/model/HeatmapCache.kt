package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

/** 热力图缓存，每天签到多少分钟 + 拉取时间 */
@JsonClass(generateAdapter = true)
data class HeatmapCache(
    val fetchedAt: Long = 0,
    val days: Map<String, Int> = emptyMap()
)
