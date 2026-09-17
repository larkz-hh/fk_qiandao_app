package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComplaintResponse(
    val code: Int?,
    val msg: String?
)
