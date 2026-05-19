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
import io.github.yesterdaylz.kxqiandao.data.model.ComplaintRequestBody
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.data.model.UserId
import io.github.yesterdaylz.kxqiandao.data.remote.RetrofitClient
import io.github.yesterdaylz.kxqiandao.receiver.SignOutReceiver
import io.github.yesterdaylz.kxqiandao.service.AutoService
import io.github.yesterdaylz.kxqiandao.util.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.collections.filter

class MainViewModel : ViewModel() {

    private val prefs = KexieApplication.instance.preferencesManager

    private val _uid = MutableStateFlow("")
    val uid: StateFlow<String> = _uid.asStateFlow()

    private val _uname = MutableStateFlow("")
    val uname: StateFlow<String> = _uname.asStateFlow()

    private val _time = MutableStateFlow("本次签到开始时间: null")
    val time: StateFlow<String> = _time.asStateFlow()

    private val _totalTime = MutableStateFlow("null")
    val totalTime: StateFlow<String> = _totalTime.asStateFlow()

    private val _week = MutableStateFlow("null")
    val week: StateFlow<String> = _week.asStateFlow()

    private val _online = MutableStateFlow(false)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val _onlineUserList = MutableStateFlow<List<ApiResponse.MemberData>>(emptyList())
    val onlineUserList: StateFlow<List<ApiResponse.MemberData>> = _onlineUserList.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _topFiveList = MutableStateFlow<List<TopFiveItem>>(emptyList())
    val topFiveList: StateFlow<List<TopFiveItem>> = _topFiveList.asStateFlow()

    private var scheduledSignOutJob: Job? = null

    companion object {
        val alarmSignOutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    //过滤后的在线用户列表
    val filteredOnlineUsers: StateFlow<List<ApiResponse.MemberData>> =
        combine(_onlineUserList, _searchText) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                (it.userName?.contains(query, true) == true) ||
                        (it.userDept?.contains(query, true) == true) ||
                        (it.userId?.toString()?.contains(query, true) == true) ||
                        (it.userLocation?.contains(query, true) == true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkOnlineStatus()

        //监听来自后台广播接收器的签退成功事件
        viewModelScope.launch {
            alarmSignOutEvent.collect {
                //收到信号后，强行在本地UI变为离线状态
                _online.value = false
                _onlineUserList.value = emptyList()
                _time.value = "本次签到开始时间: null"
                checkOnlineStatus()
            }
        }
    }

    ///检查登录和签到状态
    fun checkOnlineStatus() {
        viewModelScope.launch {
            val savedUid = prefs.uid.first()
            if (savedUid == "null") {
                _uid.value = "null"
                _uname.value = "null"
                _time.value = "本次签到开始时间: null"
                _totalTime.value = "null"
                _week.value = "null"
            } else {
                _uid.value = savedUid
                fetchOnlineStatus(savedUid)
            }
            _toastMessage.emit(Constants.INIT_ONLINE_OK)
        }
    }


    ///实际在线状态
    private suspend fun fetchOnlineStatus(uid: String) {
        try {
            val response = RetrofitClient.api.online(uid)
            if (response.isSuccessful) {
                val json = JSONObject(response.body()!!.string())
                if (json.getInt("code") == 0) {
                    val data = json.getJSONObject("data")
                    _uid.value = data.getString("userId")
                    _uname.value = data.getString("userName")
                    val status = data.getInt("status")
                    if (status == 1) {//在线

                        val serverStart = data.optString("start", "")

                        if (serverStart.isNotEmpty()) {
                            _time.value = "本次签到开始时间: $serverStart"
                            prefs.saveStartTime(serverStart)
                        } else {
                            // 我也不知道什么情况，有一个情况下日志会显示后端没给时间，结果直接走catch路线了，就先从 DataStore 的时间吧
                            val savedStartTime = prefs.startTime.first()
                            if (savedStartTime != "null" && savedStartTime.isNotBlank()) {
                                _time.value = "本次签到开始时间: $savedStartTime"
                            } else {
                                _time.value = "本次签到开始时间: 暂无数据"
                            }
                        }
                        _online.value = true
//                       prefs.saveStartTime(serverStart)
                        fetchOnlineUsers()
                    } else {//不在线
                        _time.value = "本次签到开始时间: null"
                        _online.value = false
                        prefs.saveStartTime("null")
                        _onlineUserList.value = emptyList()
                    }
                    _totalTime.value = prefs.totalTime.first()
                    _week.value = prefs.week.first()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _toastMessage.emit(Constants.NETWORK_ERROR)
            val savedStartTime = prefs.startTime.first()
            if (savedStartTime != "null" && savedStartTime.isNotBlank()) {
                _time.value = "本次签到开始时间: $savedStartTime"
                // 请求失败但本地有时间记录时，大概率处于在线（签到）状态,额maybe，似乎存在一些离谱状况，后期再考虑吧
                _online.value = true
                fetchOnlineUsers()//很神奇，加上这段重启之后才有列表（你还在签到），理论上不应该走这里啊...
            } else {
                _time.value = "本次签到开始时间: null"
                _online.value = false
            }
        }
    }

    ///判断签到签退
    fun signInOrOut() {
        val currentUid = _uid.value
        if (currentUid.isEmpty() || currentUid == "null") {
            viewModelScope.launch { _toastMessage.emit("请先输入学号") }
            return
        }
        if (_online.value) {
            signOut(currentUid)
        } else {
            signIn(currentUid)
        }
    }

    ///签到，上工!
    private fun signIn(uid: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.signIn(UserId(uid))
                val json = JSONObject(response.body()!!.string())
                when (json.getInt("code")) {
                    0 -> {
                        val data = json.getJSONObject("data")
                        _uid.value = data.getString("userId")
                        _uname.value = data.getString("userName")
                        _totalTime.value = data.getString("totalTime")
                        _week.value = data.getString("week")
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val formattedTime = formatter.format(Date())
                        _time.value = "本次签到开始时间: $formattedTime"
                        prefs.saveUid(_uid.value)
                        prefs.saveUname(_uname.value)
                        prefs.saveTotalTime(_totalTime.value)
                        prefs.saveWeek(_week.value)
                        prefs.saveStartTime(formattedTime)
                        _online.value = true
                        _toastMessage.emit("签到成功，卷🐕上工")
                        fetchOnlineUsers()
                    }

                    -201 -> {
                        _online.value = true
                        _toastMessage.emit("重复签到！")
                        fetchOnlineUsers()
                    }

                    -205 -> _toastMessage.emit("宁操作太频繁了\n请稍后重试")
                    -206 -> _toastMessage.emit("别熬夜鸭，该时段不允许签到哦")
                    else -> _toastMessage.emit("签到失败！")
                }
            } catch (e: Exception) {
                _toastMessage.emit(Constants.NETWORK_ERROR)
            }
        }
    }

    ///签退，下班！
    private fun signOut(uid: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.signOut(UserId(uid))
                val json = JSONObject(response.body()!!.string())
                when (json.getInt("code")) {
                    0 -> {
                        val data = json.getJSONObject("data")
                        _uid.value = data.getString("userId")
                        _uname.value = data.getString("userName")
                        _totalTime.value = data.getString("totalTime")
                        _week.value = data.getString("week")
                        _time.value = "本次签到时长: ${data.getString("accumulatedTime")}"
                        prefs.saveUid(_uid.value)
                        prefs.saveUname(_uname.value)
                        prefs.saveStartTime("null")
                        prefs.saveTotalTime(_totalTime.value)
                        prefs.saveWeek(_week.value)
                        _online.value = false
                        _toastMessage.emit("签退成功，辛苦了")
                        stopAutoService()
                    }

                    -202 -> {
                        _online.value = false
                        _toastMessage.emit("您还没还有签到！")
                        stopAutoService()
                    }

                    else -> _toastMessage.emit("签退失败！")
                }
            } catch (e: Exception) {
                _toastMessage.emit(Constants.NETWORK_ERROR)
            }
        }
    }

    ///获取全部正在签到成员的列表
    fun fetchOnlineUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.api.recordOnline()
                println("API Response: $response")
                println("Response Body: ${response.body()}")
                if (response.isSuccessful && response.body() != null) {
                    _onlineUserList.value = response.body()!!.data ?: emptyList()
                }
            } catch (e: Exception) {
                _toastMessage.emit("获取在线用户列表失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    ///举办成员
    fun complaint(item: ApiResponse.MemberData) {
        viewModelScope.launch {
            try {
                val body = ComplaintRequestBody(
                    targetUserId = item.userId.toString(),
                    operatorUserId = prefs.uid.first()
                )
                val response = RetrofitClient.api.complaint(body)
                if (response.isSuccessful) {
                    val json = JSONObject(response.body()!!.string())
                    if (json.getString("msg") == "举报成功") {
                        _toastMessage.emit("举报成功")
                        checkOnlineStatus()
                    } else {
                        _toastMessage.emit("举报失败")
                    }
                } else {
                    _toastMessage.emit("举报失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit(Constants.NETWORK_ERROR)
            }
        }
    }
    ///获取排名数据
    fun fetchTopFive() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.api.topFive()
                if (response.isSuccessful && response.body() != null) {
                    _topFiveList.value = response.body()?.data ?: emptyList()
                } else {
                    _toastMessage.emit("获取排行榜失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit("网络异常，无法获取排行榜")
            } finally {
                _isLoading.value = false
            }
        }
    }
    ///帮他人签退
    fun signOutOther(targetUid: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.signOut(UserId(targetUid))
                val json = JSONObject(response.body()!!.string())
                when (json.getInt("code")) {
                    0 -> {
                        _toastMessage.emit("成功将该成员签退，谢谢你，好心人😊")
                        //随便弄的，反正我觉得没有神人会在自己签到页面在在线成员列表点自己的签退按钮
                        val currentUid = prefs.uid.first()
                        if (targetUid == currentUid || targetUid == _uid.value) {
                            checkOnlineStatus()
                        } else {
                            fetchOnlineUsers()
                        }
                    }
                    -202 -> {
                        _toastMessage.emit("该成员暂未签到！")
                    }
                    else -> _toastMessage.emit("签退失败！")
                }
            } catch (e: Exception) {
                _toastMessage.emit(Constants.NETWORK_ERROR)
            }
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

        //如果设定的时间早于当前时间，认为是设定到明天的这个时间（难绷，那时你早被下线了，这个是ai手贱加上去的，不过我觉得还好）
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

    ///学号变化
    fun onUidChanged(text: String) {
        _uid.value = text
    }

    ///搜索框关键词变化
    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }
}
