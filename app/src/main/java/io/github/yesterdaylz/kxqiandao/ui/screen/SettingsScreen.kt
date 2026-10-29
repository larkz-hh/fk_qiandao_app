package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.ui.theme.ThemeMode
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.UrlRouter
import io.github.yesterdaylz.kxqiandao.viewmodel.SettingsUiState
import io.github.yesterdaylz.kxqiandao.viewmodel.SettingsViewModel
import java.util.Locale

/**
 * 设置页
 */
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    val colors = KxTheme.colors
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(text = "设置", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "外观", fontSize = 13.sp, color = colors.muted)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column {
                    ThemeOption(
                        label = "跟随系统",
                        selected = uiState.themeMode == ThemeMode.System,
                        onClick = { viewModel.setThemeMode(ThemeMode.System) }
                    )
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                    ThemeOption(
                        label = "浅色",
                        selected = uiState.themeMode == ThemeMode.Light,
                        onClick = { viewModel.setThemeMode(ThemeMode.Light) }
                    )
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                    ThemeOption(
                        label = "深色",
                        selected = uiState.themeMode == ThemeMode.Dark,
                        onClick = { viewModel.setThemeMode(ThemeMode.Dark) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "其他", fontSize = 13.sp, color = colors.muted)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                SettingsRow(
                    label = "清理缓存",
                    value = formatSize(uiState.cacheSizeBytes),
                    onClick = viewModel::clearCaches
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                SettingsRow(
                    label = "网页版",
                    onClick = { UrlRouter.openExternally(context, Constants.BASE_URL.toUri()) }
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                SettingsRow(label = "关于", onClick = onOpenAbout)
            }
        }
    }
}

/** 设置里的一行 */
@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = KxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (enabled) colors.onSurface else colors.muted,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(text = value, fontSize = 12.sp, color = colors.muted)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
}
