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
import io.github.yesterdaylz.kxqiandao.data.remote.ApiException
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.receiver.SignOutReceiver
import io.github.yesterdaylz.kxqiandao.service.AutoService
import io.github.yesterdaylz.kxqiandao.util.CacheEventBus
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.NetworkUtil
import io.github.yesterdaylz.kxqiandao.util.ToastBus
import io.github.yesterdaylz.kxqiandao.util.errorText
import io.github.yesterdaylz.kxqiandao.util.userMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.filter
import kotlin.time.Duration.Companion.milliseconds

private const val TIME_NULL = "本次签到开始时间: null"

private const val REFRESH_THROTTLE_MS = 1500L

data class SignUiState(
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
    val searchText: String = "",
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

class SignViewModel : ViewModel() {

    private val repo = SignRepository()

    private val _uiState = MutableStateFlow(SignUiState())
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()

    private var scheduledSignOutJob: Job? = null

    private var uidDirty = false

    private var lastRefreshAt = 0L

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
    }

    override fun onCleared() {
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        super.onCleared()
    }

    fun refreshNetState() {
        val state = NetworkUtil.currentState(KexieApplication.instance)
        _uiState.update { it.copy(netState = state) }
    }

    fun refreshStatus() {
        refreshNetState()
        checkOnlineStatus()
    }

    private fun onNetworkChanged() {
        viewModelScope.launch {
            delay(500.milliseconds)
            refreshNetState()
            checkOnlineStatus(silent = true)
        }
    }

    /** 检查登录和签到状态 */
    fun checkOnlineStatus(silent: Boolean = false) {
        val now = System.currentTimeMillis()
        if (silent && now - lastRefreshAt < REFRESH_THROTTLE_MS) return
        lastRefreshAt = now

        viewModelScope.launch {
            val savedUid = repo.savedUid()
            if (savedUid == "null" || savedUid.isBlank()) {
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
            if (!silent) ToastBus.send(Constants.INIT_ONLINE_OK)
        }
    }

    /** 实际在线状态 */
    private suspend fun fetchOnlineStatus(uid: String, silent: Boolean = false) {
        if (uid.isBlank() || uid == "null") return
        val result = repo.onlineStatus(uid)
        val body = result.getOrNull()

        if (body != null && body.code != 0) return

        val data = body?.data
        if (data == null) {
            val error = result.exceptionOrNull()
            if (error is ApiException) {
                if (!silent) ToastBus.send(error.userMessage("查询失败"))
            } else {
                error?.printStackTrace()
                _uiState.update { it.copy(statusKnown = false) }
                if (!silent) ToastBus.send(Constants.NETWORK_ERROR)
            }
            return
        }

        val totalTime = repo.savedTotalTime()
        val savedWeek = repo.savedWeek()
        val serverUid = data.userId?.takeIf { it.isNotBlank() }
        val serverName = data.userName?.takeIf { it.isNotBlank() }

        if (data.status == 1) {//在线
            val serverStart = data.start.orEmpty()
            val startTime = if (serverStart.isNotEmpty()) {
                repo.saveStartTime(serverStart)
                serverStart
            } else {
                repo.ongoingStartTime()
                    ?: fetchStartTimeFromRecords(uid)
                    ?: "暂无数据"
            }
            serverName?.let { repo.saveUname(it) }
            _uiState.update { state ->
                state.copy(
                    uid = if (uidDirty) state.uid else (serverUid ?: state.uid),
                    selfUid = serverUid ?: state.selfUid,
                    uname = serverName
                        ?: state.uname.takeIf { (serverUid ?: uid) == state.selfUid }
                        ?: "",
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
                    uid = if (uidDirty) state.uid else (serverUid ?: state.uid),
                    selfUid = serverUid ?: state.selfUid,
                    uname = serverName
                        ?: state.uname.takeIf { (serverUid ?: uid) == state.selfUid }
                        ?: "",
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

    private suspend fun fetchStartTimeFromRecords(uid: String): String? {
        val start = repo.records(uid).getOrNull()
            ?.firstOrNull { it.status == "在线" }
            ?.start
            ?.takeIf { it.isNotBlank() }
            ?: return null
        repo.saveStartTime(start)
        return start
    }

    /** 判断签到签退 */
    fun signInOrOut() {
        val currentUid = _uiState.value.uid
        if (currentUid.isEmpty() || currentUid == "null") {
            ToastBus.send("请先输入学号")
            return
        }
        val netState = NetworkUtil.currentState(KexieApplication.instance)
        _uiState.update { it.copy(netState = netState) }
        if (netState == NetworkUtil.NetState.Offline) {
            ToastBus.send("网络不可用，请检查网络")
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
                                uidDirty = false
                                _uiState.update {
                                    it.copy(online = true, selfUid = uid, statusKnown = true)
                                }
                                ToastBus.send("已处于签到状态，正在同步信息")
                                fetchOnlineStatus(uid)
                                return@onSuccess
                            }
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val formattedTime = formatter.format(Date())

                            val newUid = data.userId?.takeIf { it.isNotBlank() } ?: uid
                            val cachedName = _uiState.value.uname.takeIf {
                                newUid == _uiState.value.selfUid && it.isNotBlank()
                            }
                            val newName = data.userName?.takeIf { it.isNotBlank() }
                                ?: cachedName.orEmpty()

                            repo.saveSession(
                                uid = newUid,
                                uname = newName,
                                totalTime = data.totalTime.orEmpty(),
                                week = data.week.orEmpty(),
                            )
                            repo.saveStartTime(formattedTime)
                            uidDirty = false

                            _uiState.update {
                                it.copy(
                                    uid = newUid,
                                    selfUid = newUid,
                                    uname = newName,
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                    time = "本次签到开始时间: $formattedTime",
                                    online = true,
                                    statusKnown = true,
                                )
                            }
                            ToastBus.send("签到成功，美美上工")
                            if (newName.isBlank()) fetchOnlineStatus(newUid)
                            fetchOnlineUsers()
                            repo.clearHeatmapCache()
                            repo.clearRecordsCache()
                            CacheEventBus.notifyCleared()
                        }

                        -201 -> {
                            uidDirty = false
                            _uiState.update {
                                it.copy(online = true, selfUid = uid, statusKnown = true)
                            }
                            ToastBus.send("你已处于签到状态，正在同步信息")
                            fetchOnlineStatus(uid)
                        }

                        -205 -> ToastBus.send("宁操作太频繁了\n请稍后重试")
                        -206 -> ToastBus.send("别熬夜鸭，该时段不允许签到哦")
                        else -> ToastBus.send(errorText(res.msg, "签到失败！"))
                    }
                }
                .onFailure { ToastBus.send(it.userMessage(Constants.NETWORK_ERROR)) }
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
                                ToastBus.send("签退失败！")
                                return@onSuccess
                            }
                            val newUid = data.userId?.takeIf { it.isNotBlank() } ?: uid
                            val cachedName = _uiState.value.uname.takeIf {
                                newUid == _uiState.value.selfUid && it.isNotBlank()
                            }
                            val newName = data.userName?.takeIf { it.isNotBlank() }
                                ?: cachedName.orEmpty()

                            repo.saveSession(
                                uid = newUid,
                                uname = newName,
                                totalTime = data.totalTime.orEmpty(),
                                week = data.week.orEmpty(),
                            )
                            repo.clearStartTime()
                            uidDirty = false

                            _uiState.update {
                                it.copy(
                                    uid = newUid,
                                    selfUid = newUid,
                                    uname = newName,
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                    time = "本次签到时长: ${data.accumulatedTime.orEmpty()}",
                                    online = false,
                                    statusKnown = true,
                                )
                            }
                            ToastBus.send("签退成功，辛苦了")
                            stopAutoService()
                            repo.clearHeatmapCache()
                            repo.clearRecordsCache()
                            CacheEventBus.notifyCleared()
                        }

                        -202 -> {
                            _uiState.update { it.copy(online = false, statusKnown = true) }
                            ToastBus.send("您还没有签到！")
                            stopAutoService()
                        }

                        else -> ToastBus.send(errorText(res.msg, "签退失败！"))
                    }
                }
                .onFailure { ToastBus.send(it.userMessage(Constants.NETWORK_ERROR)) }
        }
    }

    /** 获取全部正在签到成员的列表 */
    fun fetchOnlineUsers() {
        viewModelScope.launch {
            repo.onlineMembers()
                .onSuccess { users -> _uiState.update { it.copy(onlineUsers = users) } }
                .onFailure { ToastBus.send(it.userMessage("获取在线用户列表失败")) }
        }
    }

    /** 举办成员 */
    fun complaint(item: ApiResponse.MemberData) {
        viewModelScope.launch {
            val operatorUid = repo.savedUid()
            repo.complaint(targetUid = item.userId.toString(), operatorUid = operatorUid)
                .onSuccess { msg ->
                    if (msg == "举报成功") {
                        ToastBus.send("举报成功")
                        checkOnlineStatus()
                    } else {
                        ToastBus.send("举报失败")
                    }
                }
                .onFailure { ToastBus.send(it.userMessage(Constants.NETWORK_ERROR)) }
        }
    }

    /** 帮他签退 */
    fun signOutOther(targetUid: String) {
        viewModelScope.launch {
            repo.signOut(targetUid)
                .onSuccess { res ->
                    when (res.code) {
                        0 -> {
                            ToastBus.send("成功将该成员签退，谢谢你，好心人😊")
                            if (targetUid == repo.savedUid()) {
                                checkOnlineStatus()
                            } else {
                                fetchOnlineUsers()
                            }
                        }

                        -202 -> ToastBus.send("该成员暂未签到！")
                        else -> ToastBus.send(errorText(res.msg, "签退失败！"))
                    }
                }
                .onFailure { ToastBus.send(it.userMessage(Constants.NETWORK_ERROR)) }
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
            ToastBus.send(
                "已设置在 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 自动签退"
            )
        } catch (e: SecurityException) {
            ToastBus.send("设定失败")
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
