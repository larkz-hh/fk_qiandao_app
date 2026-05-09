package io.github.yesterdaylz.kxqiandao.data.model


import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComplaintRequestBody(
     val targetUserId: String,
     val operatorUserId: String
)