package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignResponse(
    val code: Int,
    val data: SignData?,
    val msg: String?
)

@JsonClass(generateAdapter = true)
data class SignData(
    val userId: String?,
    val userName: String?,
    val totalTime: String?,
    val week: String?,
    val start: String?,
    val accumulatedTime: String?,// 本次累计签到时长
    val status: String?
)