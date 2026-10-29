package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

/** 签到记录缓存 */
@JsonClass(generateAdapter = true)
data class RecordsCache(
    val fetchedAt: Long = 0,
    val items: List<RecordItem> = emptyList()
)
