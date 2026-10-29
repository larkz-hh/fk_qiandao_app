package io.github.yesterdaylz.kxqiandao.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yesterdaylz.kxqiandao.KexieApplication
import io.github.yesterdaylz.kxqiandao.data.remote.GitHubClient
import io.github.yesterdaylz.kxqiandao.data.repository.SignRepository
import io.github.yesterdaylz.kxqiandao.ui.theme.ThemeMode
import io.github.yesterdaylz.kxqiandao.util.CacheEventBus
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.ToastBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val updateChecking: Boolean = false,// 正在检查更新
    val updateInfo: UpdateInfo? = null,// 发现的新版本
    val cacheSizeBytes: Long = 0,// 本地缓存占用
)

class SettingsViewModel : ViewModel() {

    private val repo = SignRepository()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.themeMode.collect { key ->
                _uiState.update { it.copy(themeMode = ThemeMode.fromKey(key)) }
            }
        }
    }

    /** 修改主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.saveThemeMode(mode.key) }
    }

    /** 重新计算本地缓存占用 */
    fun refreshCacheSize() {
        viewModelScope.launch {
            val size = repo.cacheSizeBytes()
            _uiState.update { it.copy(cacheSizeBytes = size) }
        }
    }

    /** 清理缓存 */
    fun clearCaches() {
        viewModelScope.launch {
            repo.clearHeatmapCache()
            repo.clearRecordsCache()
            CacheEventBus.notifyCleared()
            _uiState.update { it.copy(cacheSizeBytes = 0) }
            ToastBus.send("缓存已清理")
        }
    }

    /** 检查 GitHub 最新 release */
    fun checkUpdate() {
        if (_uiState.value.updateChecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(updateChecking = true) }
            val result = GitHubClient.latestRelease(Constants.REPO_SLUG)
            _uiState.update { it.copy(updateChecking = false) }
            result
                .onSuccess { release ->
                    val url = release.htmlUrl.orEmpty()
                    val tag = release.tagName?.takeIf { it.isNotBlank() }
                    if (url.isBlank() || tag == null) {
                        ToastBus.send("检查更新失败")
                        return@onSuccess
                    }
                    if (compareVersion(tag, currentVersionName()) > 0) {
                        val apkUrl = release.assets
                            ?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                            ?.browserDownloadUrl
                        _uiState.update {
                            it.copy(updateInfo = UpdateInfo(tag, url, apkUrl))
                        }
                    } else {
                        ToastBus.send("已是最新版本")
                    }
                }
                .onFailure { ToastBus.send("检查更新失败") }
        }
    }

    /** 关掉更新提示 */
    fun dismissUpdate() {
        _uiState.update { it.copy(updateInfo = null) }
    }

    /** 当前版本号 */
    private fun currentVersionName(): String = runCatching {
        @Suppress("DEPRECATION")
        KexieApplication.instance.packageManager
            .getPackageInfo(KexieApplication.instance.packageName, 0).versionName
    }.getOrNull().orEmpty()

    /** 比较版本 */
    private fun compareVersion(a: String, b: String): Int {
        val left = versionParts(a)
        val right = versionParts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }

    private fun versionParts(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '+', '_')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
