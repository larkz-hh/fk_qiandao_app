package io.github.yesterdaylz.kxqiandao.viewmodel

import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.RecordItem
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.data.remote.ApiException
import io.github.yesterdaylz.kxqiandao.receiver.SignOutReceiver
import io.github.yesterdaylz.kxqiandao.service.AutoService
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.CheckInHeatmap
import io.github.yesterdaylz.kxqiandao.util.HeatmapBuilder
import io.github.yesterdaylz.kxqiandao.util.NetworkUtil
import io.github.yesterdaylz.kxqiandao.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.collections.filter
import kotlin.time.Duration.Companion.milliseconds

private const val TIME_NULL = "本次签到开始时间: null"

private const val REFRESH_THROTTLE_MS = 1500L

/** 热力图一屏显示多少个月 */
private const val HEATMAP_MONTHS = 6

/** 翻页兜底上限，防止死循环 */
private const val MAX_HEATMAP_OFFSET = 40

/** 热力图缓存的保鲜期 */
private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

data class MainUiState(
    val uid: String = "",
    val selfUid: String = "",// 本地登录的人
    val uname: String = "",
    val time: String = TIME_NULL,
    val totalTime: String = "null",
    val week: String = "null",
    val online: Boolean = false,
    val statusKnown: Boolean = true,// 服务器状态是否已确认
    val netState: NetworkUtil.NetState = NetworkUtil.NetState.Campus,// 当前网络环境
    val onlineUsers: List<ApiResponse.MemberData> = emptyList(),
    val heatmap: CheckInHeatmap? = null,// 近半年签到热力图
    val heatmapOffset: Int = 0,// 0 = 最近半年，-1 = 再往前半年
    val heatmapMinOffset: Int = 0,// 最早能翻到哪半年
    val heatmapLoading: Boolean = false,
    val records: List<RecordItem> = emptyList(),// 全部签到记录
    val recordsLoading: Boolean = false,
    val recordsStale: Boolean = false,// 列表来自本地缓存、没刷新成功
    val totalSignDays: Int = 0,// 累计签到天数
    val totalSignMinutes: Int = 0,// 累计签到分钟数
    val themeMode: ThemeMode = ThemeMode.System,// 主题模式
    val searchText: String = "",
    val topFiveList: List<TopFiveItem> = emptyList(),
) {
    val filteredUsers: List<ApiResponse.MemberData> =
        if (searchText.isBlank()) {
            onlineUsers
        } else {
            onlineUsers.filter {
                (it.userName?.contains(searchText, true) == true) ||
                        (it.userDept?.contains(searchText, true) == true) ||
                        (it.userId?.toString()?.contains(searchText, true) == true) ||
                        (it.userLocation?.contains(searchText, true) == true)
            }
        }
}

class MainViewModel : ViewModel() {

    private val repo = SignRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var scheduledSignOutJob: Job? = null

    /** 用户改过学号输入框后，刷新时不要把它覆盖掉 */
    private var uidDirty = false

    private var lastRefreshAt = 0L

    /** 热力图每天的分钟数，翻半年时复用 */
    private var heatmapDays: Map<String, Int>? = null

    /** 原始签到记录，热力图和记录列表共用一次请求 */
    private var recordItems: List<RecordItem>? = null

    private val connectivityManager =
        KexieApplication.instance.getSystemService(ConnectivityManager::class.java)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onNetworkChanged()

        override fun onLost(network: Network) = onNetworkChanged()
    }

    companion object {
        val alarmSignOutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    init {
        refreshNetState()
        checkOnlineStatus()

        // 监听来自后台广播接收器的签退成功事件
        viewModelScope.launch {
            alarmSignOutEvent.collect {
                // 收到信号后，强行在本地UI变为离线状态
                _uiState.update {
                    it.copy(online = false, onlineUsers = emptyList(), time = TIME_NULL)
                }
                checkOnlineStatus(silent = true)
            }
        }

        runCatching { connectivityManager?.registerDefaultNetworkCallback(networkCallback) }

        // 主题设置变化时同步到 UI 状态
        viewModelScope.launch {
            repo.themeMode.collect { key ->
                _uiState.update { it.copy(themeMode = ThemeMode.fromKey(key)) }
            }
        }
    }

    /** 修改主题模式，存到 DataStore */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.saveThemeMode(mode.key) }
    }

    override fun onCleared() {
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        super.onCleared()
    }

    /** 重新判断网络环境 */
    fun refreshNetState() {
        val state = NetworkUtil.currentState(KexieApplication.instance)
        _uiState.update { it.copy(netState = state) }
    }

    /** 手动刷新：重判网络再拉一次状态 */
    fun refreshStatus() {
        refreshNetState()
        checkOnlineStatus()
    }

    /** 换网后状态大概率变了，等系统切完再刷 */
    private fun onNetworkChanged() {
        viewModelScope.launch {
            delay(500.milliseconds)
            refreshNetState()
            checkOnlineStatus(silent = true)
        }
    }

    /** 检查登录和签到状态 */
    fun checkOnlineStatus(silent: Boolean = false) {
        // 回前台和网络回调可能连着触发，短时间内只查一次
        val now = System.currentTimeMillis()
        if (silent && now - lastRefreshAt < REFRESH_THROTTLE_MS) return
        lastRefreshAt = now

        viewModelScope.launch {
            val savedUid = repo.savedUid()
            if (savedUid == "null") {
                _uiState.update {
                    it.copy(
                        uid = "null",
                        uname = "null",
                        time = TIME_NULL,
                        totalTime = "null",
                        week = "null",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        uid = if (uidDirty) it.uid else savedUid,
                        selfUid = savedUid,
                    )
                }
                fetchOnlineStatus(savedUid, silent)
            }
            if (!silent) _toastMessage.emit(Constants.INIT_ONLINE_OK)
        }
    }

    /** 实际在线状态 */
    private suspend fun fetchOnlineStatus(uid: String, silent: Boolean = false) {
        val result = repo.onlineStatus(uid)
        val body = result.getOrNull()

        if (body != null && body.code != 0) return

        val data = body?.data
        if (data == null) {
            val error = result.exceptionOrNull()
            if (error is ApiException) {
                // 服务端有回应，说明网络是通的
                if (!silent) _toastMessage.emit(error.userMessage("查询失败"))
            } else {
                // 查不到标注未确认
                error?.printStackTrace()
                _uiState.update { it.copy(statusKnown = false) }
                if (!silent) _toastMessage.emit(Constants.NETWORK_ERROR)
            }
            return
        }

        val totalTime = repo.savedTotalTime()
        val savedWeek = repo.savedWeek()

        if (data.status == 1) {//在线
            val serverStart = data.start.orEmpty()
            val startTime = if (serverStart.isNotEmpty()) {
                repo.saveStartTime(serverStart)
                serverStart
            } else {
                // 在线状态接口不返回开始时间，先从本地拿，再不行去签到记录里找
                repo.ongoingStartTime()
                    ?: fetchStartTimeFromRecords(uid)
                    ?: "暂无数据"
            }
            repo.saveUname(data.userName.orEmpty())
            _uiState.update { state ->
                state.copy(
                    uid = if (uidDirty) state.uid else data.userId.orEmpty(),
                    selfUid = data.userId.orEmpty(),
                    uname = data.userName.orEmpty(),
                    time = "本次签到开始时间: $startTime",
                    totalTime = totalTime,
                    week = data.week?.toString() ?: savedWeek,
                    online = true,
                    statusKnown = true,
                )
            }
            fetchOnlineUsers()
        } else {
            //不在线
            repo.clearStartTime()
            _uiState.update { state ->
                state.copy(
                    uid = if (uidDirty) state.uid else data.userId.orEmpty(),
                    selfUid = data.userId.orEmpty(),
                    uname = data.userName.orEmpty(),
                    time = TIME_NULL,
                    totalTime = totalTime,
                    week = data.week?.toString() ?: savedWeek,
                    online = false,
                    statusKnown = true,
                    onlineUsers = emptyList(),
                )
            }
        }
    }

    /** 从签到记录里取当前这条的开始时间 */
    private suspend fun fetchStartTimeFromRecords(uid: String): String? {
        val start = repo.records(uid).getOrNull()
            ?.firstOrNull { it.status == "在线" }
            ?.start
            ?.takeIf { it.isNotBlank() }
            ?: return null
        repo.saveStartTime(start)
        return start
    }

    /** 服务端报错时优先用它自己的提示 */
    private fun Throwable.userMessage(fallback: String): String =
        if (this is ApiException) message?.takeIf { it.isNotBlank() } ?: fallback else fallback

    /** 响应里带了 msg 就用它 */
    private fun errorText(msg: String?, fallback: String): String =
        msg?.takeIf { it.isNotBlank() } ?: fallback

    /** 判断签到签退 */
    fun signInOrOut() {
        val currentUid = _uiState.value.uid
        if (currentUid.isEmpty() || currentUid == "null") {
            viewModelScope.launch { _toastMessage.emit("请先输入学号") }
            return
        }
        val netState = NetworkUtil.currentState(KexieApplication.instance)
        _uiState.update { it.copy(netState = netState) }
        if (netState == NetworkUtil.NetState.Offline) {
            viewModelScope.launch { _toastMessage.emit("网络不可用，请检查网络") }
            return
        }
        if (_uiState.value.online) {
            signOut(currentUid)
        } else {
            signIn(currentUid)
        }
    }

    /** 签到，上工 */
    private fun signIn(uid: String) {
        viewModelScope.launch {
            repo.signIn(uid)
                .onSuccess { res ->
                    when (res.code) {
                        0 -> {
                            val data = res.data
                            if (data == null) {
                                _toastMessage.emit("签到失败！")
                                return@onSuccess
                            }
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val formattedTime = formatter.format(Date())

                            repo.saveSession(
                                uid = data.userId.orEmpty(),
                                uname = data.userName.orEmpty(),
                                totalTime = data.totalTime.orEmpty(),
                                week = data.week.orEmpty(),
                            )
                            repo.saveStartTime(formattedTime)
                            uidDirty = false

                            _uiState.update {
                                it.copy(
                                    uid = data.userId.orEmpty(),
                                    selfUid = data.userId.orEmpty(),
                                    uname = data.userName.orEmpty(),
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                    time = "本次签到开始时间: $formattedTime",
                                    online = true,
                                    statusKnown = true,
                                )
                            }
                            _toastMessage.emit("签到成功，美美上工")
                            fetchOnlineUsers()
                            // 多了一条记录，热力图缓存作废
                            heatmapDays = null
                            recordItems = null
                            repo.clearHeatmapCache()
                            repo.clearRecordsCache()
                        }

                        -201 -> {
                            // 重复签到，补全数据
                            uidDirty = false
                            _uiState.update {
                                it.copy(online = true, selfUid = uid, statusKnown = true)
                            }
                            _toastMessage.emit("你已处于签到状态，正在同步信息")
                            fetchOnlineStatus(uid)
                        }

                        -205 -> _toastMessage.emit("宁操作太频繁了\n请稍后重试")
                        -206 -> _toastMessage.emit("别熬夜鸭，该时段不允许签到哦")
                        else -> _toastMessage.emit(errorText(res.msg, "签到失败！"))
                    }
                }
                .onFailure { _toastMessage.emit(it.userMessage(Constants.NETWORK_ERROR)) }
        }
    }

    /** 签退，下班 */
    private fun signOut(uid: String) {
        viewModelScope.launch {
            repo.signOut(uid)
                .onSuccess { res ->
                    when (res.code) {
                        0 -> {
                            val data = res.data
                            if (data == null) {
                                _toastMessage.emit("签退失败！")
                                return@onSuccess
                            }
                            repo.saveSession(
                                uid = data.userId.orEmpty(),
                                uname = data.userName.orEmpty(),
                                totalTime = data.totalTime.orEmpty(),
                                week = data.week.orEmpty(),
                            )
                            repo.clearStartTime()
                            uidDirty = false

                            _uiState.update {
                                it.copy(
                                    uid = data.userId.orEmpty(),
                                    selfUid = data.userId.orEmpty(),
                                    uname = data.userName.orEmpty(),
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                    time = "本次签到时长: ${data.accumulatedTime.orEmpty()}",
                                    online = false,
                                    statusKnown = true,
                                )
                            }
                            _toastMessage.emit("签退成功，辛苦了")
                            stopAutoService()
                            heatmapDays = null
                            recordItems = null
                            repo.clearHeatmapCache()
                            repo.clearRecordsCache()
                        }

                        -202 -> {
                            _uiState.update { it.copy(online = false, statusKnown = true) }
                            _toastMessage.emit("您还没有签到！")
                            stopAutoService()
                        }

                        else -> _toastMessage.emit(errorText(res.msg, "签退失败！"))
                    }
                }
                .onFailure { _toastMessage.emit(it.userMessage(Constants.NETWORK_ERROR)) }
        }
    }

    /** 获取全部正在签到成员的列表 */
    fun fetchOnlineUsers() {
        viewModelScope.launch {
            repo.onlineMembers()
                .onSuccess { users -> _uiState.update { it.copy(onlineUsers = users) } }
                .onFailure { _toastMessage.emit(it.userMessage("获取在线用户列表失败")) }
        }
    }

    /** 举办成员 */
    fun complaint(item: ApiResponse.MemberData) {
        viewModelScope.launch {
            val operatorUid = repo.savedUid()
            repo.complaint(targetUid = item.userId.toString(), operatorUid = operatorUid)
                .onSuccess { msg ->
                    if (msg == "举报成功") {
                        _toastMessage.emit("举报成功")
                        checkOnlineStatus()
                    } else {
                        _toastMessage.emit("举报失败")
                    }
                }
                .onFailure { _toastMessage.emit(it.userMessage(Constants.NETWORK_ERROR)) }
        }
    }

    /** 获取排名数据 */
    fun fetchTopFive() {
        viewModelScope.launch {
            repo.topFive()
                .onSuccess { list -> _uiState.update { it.copy(topFiveList = list) } }
                .onFailure {
                    it.printStackTrace()
                    _toastMessage.emit(it.userMessage("获取排行榜失败"))
                }
        }
    }

    /** 热力图有本地缓存先显示，超过 6 小时或强制刷新才去拉记录 */
    fun fetchHeatmap(force: Boolean = false) {
        if (_uiState.value.heatmapLoading) return
        if (!force && heatmapDays != null) return
        viewModelScope.launch {
            if (!force) {
                val cached = repo.heatmapCache()
                if (cached != null) {
                    applyHeatmapDays(cached.days)
                    if (System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) return@launch
                }
            }
            _uiState.update { it.copy(heatmapLoading = true) }
            val uid = repo.savedUid()
            if (uid == "null") {
                _uiState.update { it.copy(heatmapLoading = false) }
                return@launch
            }
            val records = repo.records(uid).getOrElse { error ->
                _uiState.update { it.copy(heatmapLoading = false) }
                _toastMessage.emit(error.userMessage("获取签到记录失败"))
                return@launch
            }
            recordItems = records
            val days = HeatmapBuilder.dailyMinutes(records)
            repo.saveHeatmapCache(days)
            repo.saveRecordsCache(records)
            applyHeatmapDays(days)
            if (force) _toastMessage.emit("签到记录已刷新")
        }
    }

    /** 拉全部签到记录，记录列表页用；有缓存先显示，支持离线 */
    fun fetchRecords(force: Boolean = false) {
        if (_uiState.value.recordsLoading) return
        val memory = recordItems
        if (!force && memory != null) {
            _uiState.update { it.copy(records = memory) }
            return
        }
        viewModelScope.launch {
            if (!force) {
                val cached = repo.recordsCache()
                if (cached != null) {
                    recordItems = cached.items
                    _uiState.update { it.copy(records = cached.items) }
                    if (System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) return@launch
                }
            }
            _uiState.update { it.copy(recordsLoading = true) }
            val uid = repo.savedUid()
            val records = if (uid == "null") {
                emptyList()
            } else {
                repo.records(uid).getOrElse { error ->
                    _uiState.update {
                        it.copy(recordsLoading = false, recordsStale = recordItems != null)
                    }
                    if (recordItems == null) {
                        _toastMessage.emit(error.userMessage("获取签到记录失败"))
                    }
                    return@launch
                }
            }
            recordItems = records
            repo.saveRecordsCache(records)
            // 顺手把热力图和累计数据也更新了，免得两个页面不一致
            val days = HeatmapBuilder.dailyMinutes(records)
            repo.saveHeatmapCache(days)
            applyHeatmapDays(days)
            _uiState.update {
                it.copy(records = records, recordsLoading = false, recordsStale = false)
            }
            if (force) _toastMessage.emit("签到记录已刷新")
        }
    }

    /** 往前/往后翻半年，step 为 -1 表示更早的半年 */
    fun shiftHeatmap(step: Int) {
        val state = _uiState.value
        val target = (state.heatmapOffset + step).coerceIn(state.heatmapMinOffset, 0)
        if (target == state.heatmapOffset) return
        rebuildHeatmap(target)
    }

    private fun applyHeatmapDays(days: Map<String, Int>) {
        heatmapDays = days
        val minOffset = days.keys.minOrNull()?.let(::minHeatmapOffset) ?: 0
        _uiState.update {
            it.copy(
                heatmapMinOffset = minOffset,
                heatmapOffset = it.heatmapOffset.coerceIn(minOffset, 0),
                heatmapLoading = false,
                totalSignDays = days.size,
                totalSignMinutes = days.values.sum(),
            )
        }
        rebuildHeatmap(_uiState.value.heatmapOffset)
    }

    /** 最早那条记录落在哪半年 */
    private fun minHeatmapOffset(earliest: String): Int {
        var offset = 0
        while (offset > -MAX_HEATMAP_OFFSET && windowStart(offset) > earliest) {
            offset--
        }
        return offset
    }

    /** 某个半年窗口的起始日（周一），yyyy-MM-dd */
    private fun windowStart(offset: Int): String {
        val anchor = Calendar.getInstance().apply { add(Calendar.MONTH, offset * HEATMAP_MONTHS) }
        val start = (anchor.clone() as Calendar).apply { add(Calendar.MONTH, -HEATMAP_MONTHS) }
        start.add(Calendar.DAY_OF_YEAR, -((start.get(Calendar.DAY_OF_WEEK) + 5) % 7))
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(start.time)
    }

    private fun rebuildHeatmap(offset: Int) {
        val days = heatmapDays ?: return
        val anchor = Calendar.getInstance().apply { add(Calendar.MONTH, offset * HEATMAP_MONTHS) }
        _uiState.update {
            it.copy(
                heatmap = HeatmapBuilder.build(days, HEATMAP_MONTHS, anchor),
                heatmapOffset = offset,
                heatmapLoading = false,
            )
        }
    }

    /** 帮他签退 */
    fun signOutOther(targetUid: String) {
        viewModelScope.launch {
            repo.signOut(targetUid)
                .onSuccess { res ->
                    when (res.code) {
                        0 -> {
                            _toastMessage.emit("成功将该成员签退，谢谢你，好心人😊")
                            // 自己的按钮已隐藏
                            if (targetUid == repo.savedUid()) {
                                checkOnlineStatus()
                            } else {
                                fetchOnlineUsers()
                            }
                        }

                        -202 -> _toastMessage.emit("该成员暂未签到！")
                        else -> _toastMessage.emit(errorText(res.msg, "签退失败！"))
                    }
                }
                .onFailure { _toastMessage.emit(it.userMessage(Constants.NETWORK_ERROR)) }
        }
    }

    ///定时签退
    fun scheduleSignOut(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        //如果设定的时间早于当前时间，
        // 认为是设定到明天的这个时间
        // （其实那时你早被下线了）
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val context = KexieApplication.instance
        val alarmManager = context.getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, SignOutReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
        scheduledSignOutJob?.cancel()
        try {
            alarmManager.setExactAndAllowWhileIdle(
                RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
            startAutoService()
            viewModelScope.launch {
                _toastMessage.emit("已设置在 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 自动签退")
            }
        } catch (e: SecurityException) {
            viewModelScope.launch {
                _toastMessage.emit("设定失败")
            }
        }
    }

    private fun startAutoService() {
        val intent = Intent(KexieApplication.instance, AutoService::class.java)
        ContextCompat.startForegroundService(KexieApplication.instance, intent)
    }

    fun stopAutoService() {
        val intent = Intent(KexieApplication.instance, AutoService::class.java)
        KexieApplication.instance.stopService(intent)
    }

    /** 学号变化 */
    fun onUidChanged(text: String) {
        uidDirty = true
        _uiState.update { it.copy(uid = text) }
    }

    /** 搜索框关键词变化 */
    fun onSearchTextChanged(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }
}
