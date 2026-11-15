package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.animation.core.spring
import androidx.compose.animation.*
import android.Manifest
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.yesterdaylz.kxqiandao.ui.component.AppBottomBar
import io.github.yesterdaylz.kxqiandao.ui.component.AppTopBar
import io.github.yesterdaylz.kxqiandao.ui.navigation.AppDestination
import io.github.yesterdaylz.kxqiandao.ui.navigation.LocalTabNavigator
import io.github.yesterdaylz.kxqiandao.ui.navigation.rememberTabNavState
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.util.CacheEventBus
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.PermissionUtil
import io.github.yesterdaylz.kxqiandao.util.ToastBus
import io.github.yesterdaylz.kxqiandao.util.UrlRouter
import io.github.yesterdaylz.kxqiandao.util.showToast
import io.github.yesterdaylz.kxqiandao.viewmodel.MineViewModel
import io.github.yesterdaylz.kxqiandao.viewmodel.RankingViewModel
import io.github.yesterdaylz.kxqiandao.viewmodel.SettingsViewModel
import io.github.yesterdaylz.kxqiandao.viewmodel.SignViewModel
import java.util.Calendar

@Composable
fun MainScreen(
    signViewModel: SignViewModel = viewModel(),
    rankingViewModel: RankingViewModel = viewModel(),
    mineViewModel: MineViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val signState by signViewModel.uiState.collectAsStateWithLifecycle()
    val rankingState by rankingViewModel.uiState.collectAsStateWithLifecycle()
    val mineState by mineViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    /// 收集 Toast 事件
    LaunchedEffect(Unit) {
        ToastBus.messages.collect { msg ->
            if (msg.isNotEmpty()) {
                msg.showToast(context, Toast.LENGTH_SHORT)
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
        signViewModel.checkOnlineStatus(silent = true)
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

    // 设置页清理缓存时，顺手把两个 WebView 的网页缓存也清掉
    LaunchedEffect(Unit) {
        CacheEventBus.webCleared.collect {
            knowledgeHolder.clearCache(context)
            campusRunHolder.clearCache(context)
        }
    }

    // 顶栏归页面所有
    val mainTopBar: @Composable () -> Unit = {
        AppTopBar(
            onLogoDoubleTap = {
                if (selectedTab == AppDestination.CheckIn) {
                    signViewModel.refreshStatus()
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
                            signViewModel.scheduleSignOut(hourOfDay, minute)
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

    Scaffold(
        containerColor = KxTheme.colors.background,
        bottomBar = {
            if (showBars) {
                AppBottomBar(
                    selected = selectedTab,
                    onSelect = { destination ->
                        selectedTab = destination
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
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                popTransitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                entryProvider = entryProvider {
                    entry<AppDestination.CheckIn> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            mainTopBar()
                            CheckInTab(
                                padding = PaddingValues(bottom = padding.calculateBottomPadding()),
                                uiState = signState,
                                viewModel = signViewModel
                            )
                        }
                    }
                    entry<AppDestination.Ranking> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            mainTopBar()
                            RankingTab(
                                padding = PaddingValues(bottom = padding.calculateBottomPadding()),
                                uiState = rankingState,
                                onLoad = { rankingViewModel.loadTopFive() },
                                onRefresh = rankingViewModel::refresh,
                                onSelectGroup = rankingViewModel::switchGroup,
                            )
                        }
                    }
                    entry<AppDestination.Entries> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            mainTopBar()
                            EntriesTab(padding = PaddingValues(bottom = padding.calculateBottomPadding()))
                        }
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
                        Column(modifier = Modifier.fillMaxSize()) {
                            mainTopBar()
                            MineTab(
                                padding = PaddingValues(bottom = padding.calculateBottomPadding()),
                                signState = signState,
                                uiState = mineState,
                                viewModel = mineViewModel
                            )
                        }
                    }
                    entry<AppDestination.Records> {
                        RecordsScreen(
                            padding = padding,
                            uiState = mineState,
                            viewModel = mineViewModel,
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                    entry<AppDestination.Settings> {
                        SettingsScreen(
                            padding = padding,
                            uiState = settingsState,
                            viewModel = settingsViewModel,
                            onOpenAbout = { navState.navigator.navigateTo(AppDestination.About) },
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                    entry<AppDestination.About> {
                        AboutScreen(
                            padding = padding,
                            updateChecking = settingsState.updateChecking,
                            updateInfo = settingsState.updateInfo,
                            onCheckUpdate = settingsViewModel::checkUpdate,
                            onDismissUpdate = settingsViewModel::dismissUpdate,
                            onBack = { navState.navigator.goBack() }
                        )
                    }
                }
            )
        }
    }
}

