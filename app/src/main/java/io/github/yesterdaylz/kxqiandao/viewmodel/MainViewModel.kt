package io.github.yesterdaylz.kxqiandao.viewmodel

import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.receiver.SignOutReceiver
import io.github.yesterdaylz.kxqiandao.service.AutoService
import io.github.yesterdaylz.kxqiandao.util.Constants
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

private const val TIME_NULL = "本次签到开始时间: null"

data class MainUiState(
    val uid: String = "",
    val selfUid: String = "",// 本地登录的人
    val uname: String = "",
    val time: String = TIME_NULL,
    val totalTime: String = "null",
    val week: String = "null",
    val online: Boolean = false,
    val onlineUsers: List<ApiResponse.MemberData> = emptyList(),
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

    companion object {
        val alarmSignOutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    init {
        checkOnlineStatus()

        // 监听来自后台广播接收器的签退成功事件
        viewModelScope.launch {
            alarmSignOutEvent.collect {
                // 收到信号后，强行在本地UI变为离线状态
                _uiState.update {
                    it.copy(online = false, onlineUsers = emptyList(), time = TIME_NULL)
                }
                checkOnlineStatus()
            }
        }
    }

    /** 检查登录和签到状态 */
    fun checkOnlineStatus() {
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
                _uiState.update { it.copy(uid = savedUid, selfUid = savedUid) }
                fetchOnlineStatus(savedUid)
            }
            _toastMessage.emit(Constants.INIT_ONLINE_OK)
        }
    }

    /** 实际在线状态 */
    private suspend fun fetchOnlineStatus(uid: String) {
        val result = repo.onlineStatus(uid)
        val body = result.getOrNull()

        if (body != null && body.code != 0) return

        val data = body?.data
        if (data == null) {
            result.exceptionOrNull()?.printStackTrace()
            _toastMessage.emit(Constants.NETWORK_ERROR)
            val savedStartTime = repo.ongoingStartTime()
            if (savedStartTime != null) {
                // 请求失败但本地有时间记录时，大概率处于在线（签到）状态,额maybe，似乎存在一些离谱状况，后期再考虑吧
                _uiState.update {
                    it.copy(time = "本次签到开始时间: $savedStartTime", online = true)
                }
                fetchOnlineUsers()//很神奇，加上这段重启之后才有列表（你还在签到），理论上不应该走这里啊...
            } else {
                _uiState.update { it.copy(time = TIME_NULL, online = false) }
            }
            return
        }

        val totalTime = repo.savedTotalTime()
        val week = repo.savedWeek()

        if (data.status == 1) {//在线
            val serverStart = data.start.orEmpty()
            val startTime = if (serverStart.isNotEmpty()) {
                repo.saveStartTime(serverStart)
                serverStart
            } else {
                // 我也不知道什么情况，
                // 有一个情况下日志会显示后端没给时间，
                // 结果直接走catch路线了，就先从 DataStore 的时间吧
                repo.ongoingStartTime() ?: "暂无数据"
            }
            _uiState.update {
                it.copy(
                    uid = data.userId.orEmpty(),
                    selfUid = data.userId.orEmpty(),
                    uname = data.userName.orEmpty(),
                    time = "本次签到开始时间: $startTime",
                    totalTime = totalTime,
                    week = week,
                    online = true,
                )
            }
            fetchOnlineUsers()
        } else {
            //不在线
            repo.clearStartTime()
            _uiState.update {
                it.copy(
                    uid = data.userId.orEmpty(),
                    selfUid = data.userId.orEmpty(),
                    uname = data.userName.orEmpty(),
                    time = TIME_NULL,
                    totalTime = totalTime,
                    week = week,
                    online = false,
                    onlineUsers = emptyList(),
                )
            }
        }
    }

    /** 判断签到签退 */
    fun signInOrOut() {
        val currentUid = _uiState.value.uid
        if (currentUid.isEmpty() || currentUid == "null") {
            viewModelScope.launch { _toastMessage.emit("请先输入学号") }
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

                            _uiState.update {
                                it.copy(
                                    uid = data.userId.orEmpty(),
                                    selfUid = data.userId.orEmpty(),
                                    uname = data.userName.orEmpty(),
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                    time = "本次签到开始时间: $formattedTime",
                                    online = true,
                                )
                            }
                            _toastMessage.emit("签到成功，美美上工")
                            fetchOnlineUsers()
                        }

                        -201 -> {
                            _uiState.update { it.copy(online = true, selfUid = uid) }
                            _toastMessage.emit("重复签到！")
                            fetchOnlineUsers()
                        }

                        -205 -> _toastMessage.emit("宁操作太频繁了\n请稍后重试")
                        -206 -> _toastMessage.emit("别熬夜鸭，该时段不允许签到哦")
                        else -> _toastMessage.emit("签到失败！")
                    }
                }
                .onFailure { _toastMessage.emit(Constants.NETWORK_ERROR) }
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

                            _uiState.update {
                                it.copy(
                                    uid = data.userId.orEmpty(),
                                    selfUid = data.userId.orEmpty(),
                                    uname = data.userName.orEmpty(),
                                    totalTime = data.totalTime.orEmpty(),
                                    week = data.week.orEmpty(),
                                    time = "本次签到时长: ${data.accumulatedTime.orEmpty()}",
                                    online = false,
                                )
                            }
                            _toastMessage.emit("签退成功，辛苦了")
                            stopAutoService()
                        }

                        -202 -> {
                            _uiState.update { it.copy(online = false) }
                            _toastMessage.emit("您还没还有签到！")
                            stopAutoService()
                        }

                        else -> _toastMessage.emit("签退失败！")
                    }
                }
                .onFailure { _toastMessage.emit(Constants.NETWORK_ERROR) }
        }
    }

    /** 获取全部正在签到成员的列表 */
    fun fetchOnlineUsers() {
        viewModelScope.launch {
            repo.onlineMembers()
                .onSuccess { users -> _uiState.update { it.copy(onlineUsers = users) } }
                .onFailure { _toastMessage.emit("获取在线用户列表失败") }
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
                .onFailure { _toastMessage.emit(Constants.NETWORK_ERROR) }
        }
    }

    /** 获取排名数据 */
    fun fetchTopFive() {
        viewModelScope.launch {
            repo.topFive()
                .onSuccess { list -> _uiState.update { it.copy(topFiveList = list) } }
                .onFailure {
                    it.printStackTrace()
                    _toastMessage.emit("获取排行榜失败")
                }
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
                        else -> _toastMessage.emit("签退失败！")
                    }
                }
                .onFailure { _toastMessage.emit(Constants.NETWORK_ERROR) }
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
        _uiState.update { it.copy(uid = text) }
    }

    /** 搜索框关键词变化 */
    fun onSearchTextChanged(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }
}
