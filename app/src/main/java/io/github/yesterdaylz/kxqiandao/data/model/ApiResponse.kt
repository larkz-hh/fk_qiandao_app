package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse(
    val data: List<MemberData>?,
    val code: Int?,
    val msg: String?
) {
    @JsonClass(generateAdapter = true)
    data class MemberData(
        val userId: Long?,
        val userName: String?,
        val userDept: String?,
        val userLocation: String?,
        val status: Int?
    )
}