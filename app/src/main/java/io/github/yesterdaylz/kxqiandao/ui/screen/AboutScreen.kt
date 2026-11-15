package io.github.yesterdaylz.kxqiandao.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import io.github.yesterdaylz.kxqiandao.R
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.util.ApkUpdateManager
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.UrlRouter
import io.github.yesterdaylz.kxqiandao.viewmodel.UpdateInfo

/**
 * 关于页
 */
@Composable
fun AboutScreen(
    padding: PaddingValues,
    updateChecking: Boolean,
    updateInfo: UpdateInfo?,
    onCheckUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onBack: () -> Unit
) {
    val colors = KxTheme.colors
    val context = LocalContext.current
    val version = remember { appVersion(context) }

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
            Text(text = "关于", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "版本 $version", fontSize = 13.sp, color = colors.muted)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            AboutRow(
                label = "GitHub 仓库",
                onClick = { UrlRouter.openExternally(context, Constants.REPO_URL.toUri()) }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            AboutRow(
                label = "我要反馈",
                onClick = { UrlRouter.openExternally(context, Constants.ISSUES_URL.toUri()) }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            AboutRow(
                label = "检查更新",
                value = if (updateChecking) "检查中…" else null,
                enabled = !updateChecking,
                onClick = onCheckUpdate
            )
        }
    }

    updateInfo?.let { info ->
        Dialog(onDismissRequest = onDismissUpdate) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "发现新版本",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "GitHub 上发布了 ${info.version}",
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (info.apkUrl != null) {
                        Button(
                            onClick = {
                                ApkUpdateManager.start(context, info.apkUrl, info.version)
                                onDismissUpdate()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            )
                        ) { Text("应用内下载更新") }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            UrlRouter.openExternally(context, info.url.toUri())
                            onDismissUpdate()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.onSurface
                        )
                    ) { Text("去 Release 页面") }

                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onDismissUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "稍后", color = colors.muted)
                    }
                }
            }
        }
    }
}

/** 关于页里的一行 */
@Composable
private fun AboutRow(
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

private fun appVersion(context: Context): String =
    runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "—"
