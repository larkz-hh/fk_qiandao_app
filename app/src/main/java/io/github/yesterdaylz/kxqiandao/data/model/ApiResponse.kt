package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse(
    @field:Json(name = "data") val data: List<MemberData>?,
    @field:Json(name = "code") val code: Int?,
    @field:Json(name = "msg") val msg: String?
) {
    @JsonClass(generateAdapter = true)
    data class MemberData(
        @field:Json(name = "userId") val userId: String?,
        @field:Json(name = "userName") val userName: String?,
        @field:Json(name = "userDept") val userDept: String?,
        @field:Json(name = "userLocation") val userLocation: String?,
        @field:Json(name = "status") val status: Int?
    )
}