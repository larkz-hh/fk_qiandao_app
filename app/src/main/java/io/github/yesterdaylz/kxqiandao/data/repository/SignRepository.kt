package io.github.yesterdaylz.kxqiandao.data.repository

import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.local.PreferencesManager
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.ComplaintRequestBody
import io.github.yesterdaylz.kxqiandao.data.model.HeatmapCache
import io.github.yesterdaylz.kxqiandao.data.model.OnlineStatusResponse
import io.github.yesterdaylz.kxqiandao.data.model.RankingCache
import io.github.yesterdaylz.kxqiandao.data.model.RecordItem
import io.github.yesterdaylz.kxqiandao.data.model.RecordsCache
import io.github.yesterdaylz.kxqiandao.data.model.SignResponse
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.model.UserId
import io.github.yesterdaylz.kxqiandao.data.remote.ApiException
import io.github.yesterdaylz.kxqiandao.data.remote.ApiService
import io.github.yesterdaylz.kxqiandao.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.Response

class SignRepository(
    private val api: ApiService = RetrofitClient.api,
    private val prefs: PreferencesManager = KexieApplication.instance.preferencesManager,
) {

    private val heatmapAdapter by lazy {
        RetrofitClient.moshi.adapter(HeatmapCache::class.java)
    }

    private val recordsAdapter by lazy {
        RetrofitClient.moshi.adapter(RecordsCache::class.java)
    }

    private val rankingAdapter by lazy {
        RetrofitClient.moshi.adapter(RankingCache::class.java)
    }

    // 本地会话

    /** 主题模式设置 */
    val themeMode: Flow<String> = prefs.themeMode

    /** 保存主题模式 */
    suspend fun saveThemeMode(value: String) {
        prefs.saveThemeMode(value)
    }

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
        if (uname.isNotBlank()) prefs.saveUname(uname)
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

    // 热力图缓存

    /** 热力图、签到记录、排行榜缓存占的字节数 */
    suspend fun cacheSizeBytes(): Long {
        val heatmap = prefs.heatmapCache.first().toByteArray().size
        val records = prefs.recordsCache.first().toByteArray().size
        val rankingNew = prefs.rankingNewCache.first().toByteArray().size
        val rankingOld = prefs.rankingOldCache.first().toByteArray().size
        return (heatmap + records + rankingNew + rankingOld).toLong()
    }

    /** 读热力图缓存 */
    suspend fun heatmapCache(): HeatmapCache? {
        val raw = prefs.heatmapCache.first()
        if (raw.isBlank()) return null
        return runCatching { heatmapAdapter.fromJson(raw) }.getOrNull()
    }

    /** 存热力图缓存，每天多少分钟 */
    suspend fun saveHeatmapCache(days: Map<String, Int>) {
        val json = runCatching {
            heatmapAdapter.toJson(HeatmapCache(fetchedAt = System.currentTimeMillis(), days = days))
        }.getOrNull() ?: return
        prefs.saveHeatmapCache(json)
    }

    /** 清掉热力图缓存 */
    suspend fun clearHeatmapCache() {
        prefs.saveHeatmapCache("")
    }

    /** 读签到记录缓存 */
    suspend fun recordsCache(): RecordsCache? {
        val raw = prefs.recordsCache.first()
        if (raw.isBlank()) return null
        return runCatching { recordsAdapter.fromJson(raw) }.getOrNull()
    }

    /** 存签到记录缓存 */
    suspend fun saveRecordsCache(records: List<RecordItem>) {
        val compact = records.map {
            it.copy(id = null, userId = null, userName = null, userDept = null)
        }
        val json = runCatching {
            recordsAdapter.toJson(RecordsCache(fetchedAt = System.currentTimeMillis(), items = compact))
        }.getOrNull() ?: return
        prefs.saveRecordsCache(json)
    }

    /** 清掉签到记录缓存 */
    suspend fun clearRecordsCache() {
        prefs.saveRecordsCache("")
    }

    /** 读排行榜缓存，oldMan = true 老生榜 */
    suspend fun rankingCache(oldMan: Boolean): RankingCache? {
        val raw = (if (oldMan) prefs.rankingOldCache else prefs.rankingNewCache).first()
        if (raw.isBlank()) return null
        return runCatching { rankingAdapter.fromJson(raw) }.getOrNull()
    }

    /** 存排行榜缓存 */
    suspend fun saveRankingCache(oldMan: Boolean, items: List<TopFiveItem>) {
        val json = runCatching {
            rankingAdapter.toJson(RankingCache(fetchedAt = System.currentTimeMillis(), items = items))
        }.getOrNull() ?: return
        prefs.saveRankingCache(json, oldMan)
    }

    /** 清空排行榜缓存 */
    suspend fun clearRankingCache() {
        prefs.saveRankingCache("", oldMan = false)
        prefs.saveRankingCache("", oldMan = true)
    }

    // 网络

    /** 签到 */
    suspend fun signIn(uid: String): Result<SignResponse> = call { api.signIn(UserId(uid)) }

    /** 签退 */
    suspend fun signOut(uid: String): Result<SignResponse> = call { api.signOut(UserId(uid)) }

    /** 查某人的在线状态 */
    suspend fun onlineStatus(uid: String): Result<OnlineStatusResponse> = call { api.online(uid) }

    /** 查某人的全部签到记录 */
    suspend fun records(uid: String): Result<List<RecordItem>> =
        call { api.records(uid) }.map { it.data ?: emptyList() }

    /** 拉取全部正在签到的人 */
    suspend fun onlineMembers(): Result<List<ApiResponse.MemberData>> =
        call { api.recordOnline() }.map { it.data ?: emptyList() }

    /** 拉取排名 */
    suspend fun topFive(oldMan: Boolean = false): Result<List<TopFiveItem>> =
        call { api.topFive(oldMan.takeIf { it }) }.map { it.data ?: emptyList() }

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
                val error = response.errorBody()?.string()?.let { raw ->
                    runCatching { RetrofitClient.apiErrorAdapter.fromJson(raw) }.getOrNull()
                }
                Result.failure(
                    ApiException(error?.code, error?.msg ?: "请求失败 HTTP ${response.code()}")
                )
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
