package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnlineStatusResponse(
    val code: Int?,
    val msg: String?,
    val data: OnlineStatusData?
)

@JsonClass(generateAdapter = true)
data class OnlineStatusData(
    val userId: String?,
    val userName: String?,
    val status: Int?,// 1 = 在线
    val start: String?// 本次签到开始时间
)
