package io.github.yesterdaylz.kxqiandao.ui.screen

import android.Manifest
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.yesterdaylz.kxqiandao.ui.component.AppBottomBar
import io.github.yesterdaylz.kxqiandao.ui.component.AppLogo
import io.github.yesterdaylz.kxqiandao.ui.navigation.AppDestination
import io.github.yesterdaylz.kxqiandao.ui.navigation.LocalTabNavigator
import io.github.yesterdaylz.kxqiandao.ui.navigation.rememberTabNavState
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.PermissionUtil
import io.github.yesterdaylz.kxqiandao.util.UrlRouter
import io.github.yesterdaylz.kxqiandao.util.showToast
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel
import java.util.Calendar

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    /// 收集 Toast 事件
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            if (msg.isNotEmpty()) {
                msg.showToast(context,Toast.LENGTH_SHORT)
            }
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf<AppDestination>(AppDestination.CheckIn) }

    // Tab 独立回退栈
    val navState = rememberTabNavState(selectedTab)

    // Tab 根页面显示顶栏 / 底栏
    val showBars = navState.isRootPage

    // 切后台再回来，签到状态可能变化
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkOnlineStatus(silent = true)
    }

    var pendingLocationRequest by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingLocationRequest?.invoke(granted)
        pendingLocationRequest = null
    }

    // 各页各持一个 WebView，切回来不重载
    val knowledgeHolder = remember {
        WebPageViewHolder(Constants.KNOWLEDGE_BASE_URL, UrlRouter::isKnowledgeHost)
    }
    // 认证域名由服务端下发不固定，跳出 WebView 会丢登录态
    val campusRunHolder = remember {
        WebPageViewHolder(
            startUrl = Constants.CAMPUS_RUN_URL,
            isInternalHost = { true },
            fixCollapsedLayout = true,
            requestGeolocation = { onResult ->
                if (PermissionUtil.hasLocationPermission(context)) {
                    onResult(true)
                } else {
                    pendingLocationRequest = onResult
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            knowledgeHolder.destroy()
            campusRunHolder.destroy()
        }
    }

    Scaffold(
        containerColor = KxTheme.colors.background,
        topBar = {
            if (showBars) {
                MainTopBar(
                    onLogoDoubleTap = {
                        // 已经在签到页就手动刷新一次状态，否则切回签到页
                        if (selectedTab == AppDestination.CheckIn) {
                            viewModel.refreshStatus()
                        } else {
                            selectedTab = AppDestination.CheckIn
                        }
                    },
                    onScheduleSignOut = {
                        if (PermissionUtil.ensureBatteryOptimizationDisabled(context) &&
                            PermissionUtil.ensureExactAlarmPermission(context)
                        ) {
                            // 先用老 TimePickerDialog 吧
                            val calendar = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    viewModel.scheduleSignOut(hourOfDay, minute)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).apply {
                                setTitle("设置签退时间")
                                setMessage("温馨提醒，本功能启动前提：\n1. 连接在科协网里\n2. 请勿关闭通知栏中的保活通知")
                                show()
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBars) {
                AppBottomBar(
                    selected = selectedTab,
                    onSelect = { destination ->
                        selectedTab = destination
                        if (destination == AppDestination.Ranking) viewModel.fetchTopFive() //切换到该页面时刷新数据
                    }
                )
            }
        }
    ) { padding ->
        // 把当前 Tab 的导航器 provide 给 NavDisplay 里所有页面
        CompositionLocalProvider(LocalTabNavigator provides navState.navigator) {
            NavDisplay(
                backStack = navState.currentStack,
                onBack = { navState.navigator.goBack() },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                entryProvider = entryProvider {
                    entry<AppDestination.CheckIn> {
                        CheckInTab(padding = padding, uiState = uiState, viewModel = viewModel)
                    }
                    entry<AppDestination.Ranking> {
                        RankingTab(padding = padding, topFiveList = uiState.topFiveList)
                    }
                    entry<AppDestination.Entries> {
                        EntriesTab(padding = padding)
                    }
                    entry<AppDestination.Knowledge> {
                        WebPageScreen(
                            padding = padding,
                            holder = knowledgeHolder,
                            title = "知识库",
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                    entry<AppDestination.CampusRun> {
                        WebPageScreen(
                            padding = padding,
                            holder = campusRunHolder,
                            title = "校园跑",
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                    entry<AppDestination.Mine> {
                        MineTab(padding = padding, uiState = uiState, viewModel = viewModel)
                    }
                    entry<AppDestination.Records> {
                        RecordsScreen(
                            padding = padding,
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                    entry<AppDestination.Settings> {
                        SettingsScreen(
                            padding = padding,
                            themeMode = uiState.themeMode,
                            onThemeModeChange = viewModel::setThemeMode,
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                }
            )
        }
    }
}

/** 顶栏：LOGO + 标题 + 定时签退 */
@Composable
private fun MainTopBar(
    onLogoDoubleTap: () -> Unit,
    onScheduleSignOut: () -> Unit,
) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(onDoubleTap = onLogoDoubleTap)
                Spacer(modifier = Modifier.width(12.dp))
                Text("三院科协", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (Constants.ENABLE_SCHEDULED_SIGN_OUT) {
                IconButton(onClick = onScheduleSignOut) {
                    Icon(Icons.Default.Alarm, contentDescription = "定时签退")
                }
            }
        }
    }
}
