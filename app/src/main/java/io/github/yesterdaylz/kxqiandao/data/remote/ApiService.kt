package io.github.yesterdaylz.kxqiandao.data.remote

import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.ComplaintRequestBody
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveResponse
import io.github.yesterdaylz.kxqiandao.data.model.UserId
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    ///签入
    @POST("/api/user/signIn")
    suspend fun signIn(@Body userId: UserId): Response<ResponseBody>
    ///签出
    @POST("/api/user/signOut")
    suspend fun signOut(@Body userId: UserId): Response<ResponseBody>
    ///判断在线
    @GET("/api/record/online/{userId}")
    suspend fun online(@Path("userId") userId: String): Response<ResponseBody>
    ///获取在线记录
    @GET("/api/record/online")
    suspend fun recordOnline(): Response<ApiResponse>
    ///举办
    @POST("/api/user/complaint")
    suspend fun complaint(@Body body: ComplaintRequestBody): Response<ResponseBody>

    @GET("/api/record/topFive")
    suspend fun topFive(): Response<TopFiveResponse>
}