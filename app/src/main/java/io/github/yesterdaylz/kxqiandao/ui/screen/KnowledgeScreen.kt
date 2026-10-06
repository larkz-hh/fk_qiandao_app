package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.yesterdaylz.kxqiandao.ui.theme.Blue1
import io.github.yesterdaylz.kxqiandao.ui.theme.DividerGray
import io.github.yesterdaylz.kxqiandao.ui.theme.GrayOffline

/**
 * 知识库页
 */
@Composable
fun KnowledgePage(
    padding: PaddingValues,
    holder: KnowledgeWebViewHolder,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val uiState = holder.uiState

    // WebView 内部持有历史时，返回键先在页面里后退
    BackHandler(enabled = uiState.canGoBack) {
        holder.peek()?.goBack()
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
    ) {
        KnowledgeTopBar(onBack = onBack)

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> holder.obtain(ctx) }
            )

            if (uiState.loading) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    color = Blue1,              // 品牌蓝，不用主题默认的紫
                    trackColor = DividerGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            uiState.errorMessage?.let { message ->
                KnowledgeErrorView(
                    message = message,
                    onRetry = { holder.retry() },
                    onOpenInBrowser = { holder.openInBrowser(context) }
                )
            }
        }
    }
}

/** 知识库页顶栏 */
@Composable
private fun KnowledgeTopBar(onBack: () -> Unit) {
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
        Text(text = "知识库", fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

/** 主框架加载失败时的错误提示 */
@Composable
private fun KnowledgeErrorView(
    message: String,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = GrayOffline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("知识库加载失败", fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = GrayOffline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue1,
                    contentColor = Color.White
                )
            ) { Text("重新加载") }
            TextButton(
                onClick = onOpenInBrowser,
                colors = ButtonDefaults.textButtonColors(contentColor = Blue1)
            ) { Text("用浏览器打开") }
        }
    }
}
