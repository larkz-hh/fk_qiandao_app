package io.github.yesterdaylz.kxqiandao.data.repository

import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.local.PreferencesManager
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.ComplaintRequestBody
import io.github.yesterdaylz.kxqiandao.data.model.OnlineStatusResponse
import io.github.yesterdaylz.kxqiandao.data.model.SignResponse
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.model.UserId
import io.github.yesterdaylz.kxqiandao.data.remote.ApiService
import io.github.yesterdaylz.kxqiandao.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.Response

class SignRepository(
    private val api: ApiService = RetrofitClient.api,
    private val prefs: PreferencesManager = KexieApplication.instance.preferencesManager,
) {

    // 本地会话

    /** 读取本地存的学号 */
    suspend fun savedUid(): String = prefs.uid.first()

    /** 读取本地记录的、尚未结束的签到开始时间 */
    suspend fun ongoingStartTime(): String? =
        prefs.startTime.first().takeIf { it != START_TIME_NONE && it.isNotBlank() }

    /** 读取本地存的本周总时长 */
    suspend fun savedTotalTime(): String = prefs.totalTime.first()

    /** 读取本地存的周序 */
    suspend fun savedWeek(): String = prefs.week.first()

    /** 一次会话落地 */
    suspend fun saveSession(
        uid: String,
        uname: String,
        totalTime: String,
        week: String,
    ) {
        prefs.saveUid(uid)
        prefs.saveUname(uname)
        prefs.saveTotalTime(totalTime)
        prefs.saveWeek(week)
    }

    /** 补全姓名 */
    suspend fun saveUname(value: String) {
        if (value.isNotBlank()) prefs.saveUname(value)
    }

    /** 记下本次签到开始时间 */
    suspend fun saveStartTime(value: String) {
        prefs.saveStartTime(value)
    }

    /** 清掉本次签到开始时间 */
    suspend fun clearStartTime() {
        prefs.saveStartTime(START_TIME_NONE)
    }

    // 网络

    /** 签到 */
    suspend fun signIn(uid: String): Result<SignResponse> = call { api.signIn(UserId(uid)) }

    /** 签退 */
    suspend fun signOut(uid: String): Result<SignResponse> = call { api.signOut(UserId(uid)) }

    /** 查某人的在线状态 */
    suspend fun onlineStatus(uid: String): Result<OnlineStatusResponse> = call { api.online(uid) }

    /** 拉取全部正在签到的人 */
    suspend fun onlineMembers(): Result<List<ApiResponse.MemberData>> =
        call { api.recordOnline() }.map { it.data ?: emptyList() }

    /** 拉取排名 */
    suspend fun topFive(): Result<List<TopFiveItem>> =
        call { api.topFive() }.map { it.data ?: emptyList() }

    /** 举报 */
    suspend fun complaint(targetUid: String, operatorUid: String): Result<String> = call {
        api.complaint(ComplaintRequestBody(targetUserId = targetUid, operatorUserId = operatorUid))
    }.map { it.msg ?: "" }

    /** 发请求并取 body */
    private suspend fun <T> call(block: suspend () -> Response<T>): Result<T> {
        return try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IllegalStateException("HTTP ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val START_TIME_NONE = "null"
    }
}
