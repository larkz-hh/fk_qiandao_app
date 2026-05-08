package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComplaintRequestBody(
    @field:Json(name = "targetUserId") val targetUserId: String,
    @field:Json(name = "operatorUserId") val operatorUserId: String
)