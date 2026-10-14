package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecordListResponse(
    val data: List<RecordItem>?,
    val code: Int?,
    val msg: String?
)

@JsonClass(generateAdapter = true)
data class RecordItem(
    val id: String?,
    val userId: Long?,
    val userName: String?,
    val userDept: String?,
    val userLocation: String?,
    val start: String?,
    val end: String?,
    val status: String?,// 在线 / 已签退 / 被迫下线
    val accumulatedTime: String?
)
