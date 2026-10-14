package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.JsonClass

/** 接口报错时的响应体，只有 code 和 msg */
@JsonClass(generateAdapter = true)
data class ApiError(
    val code: Int?,
    val msg: String?
)
