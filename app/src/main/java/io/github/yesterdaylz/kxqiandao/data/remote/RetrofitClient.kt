package io.github.yesterdaylz.kxqiandao.data.remote

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.github.yesterdaylz.kxqiandao.data.model.ApiError
import io.github.yesterdaylz.kxqiandao.util.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    internal val moshi = Moshi.Builder().build()

    /** 解析非 2xx 响应体里的 code/msg */
    val apiErrorAdapter: JsonAdapter<ApiError> by lazy { moshi.adapter(ApiError::class.java) }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}