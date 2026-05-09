package io.github.yesterdaylz.kxqiandao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.UserId
import io.github.yesterdaylz.kxqiandao.data.remote.RetrofitClient
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
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
                        _toastMessage.emit("签退成功，卷🐕下工")
                    }

                    -202 -> {
                        _online.value = false
                        _toastMessage.emit("您还没还有签到！")
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


    ///学号变化
    fun onUidChanged(text: String) {
        _uid.value = text
    }

    ///搜索框关键词变化
    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }
}