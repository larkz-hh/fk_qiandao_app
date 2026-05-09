package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopFiveResponse(
    val data: List<TopFiveItem>?,
    val code: Int?,
    val msg: String?
)

@JsonClass(generateAdapter = true)
data class TopFiveItem(
    val userId: String?,
    val userName: String?,
    val userDept: String?,
    val userLocation: String?,
    val totalTime: String?,
    val week: Int?
)
