package io.github.yesterdaylz.kxqiandao.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.yesterdaylz.kxqiandao.util.Constants
import io.github.yesterdaylz.kxqiandao.util.UrlRouter
import androidx.core.net.toUri

data class KnowledgeUiState(
    val loading: Boolean = true,// 是否正在加载
    val progress: Int = 0,// 进度
    val canGoBack: Boolean = false,// 页面是否能后退
    val errorMessage: String? = null,// 加载失败提示文案
)

/**
 * 知识库页的 WebView 持有
 */
class KnowledgeWebViewHolder {

    // 页面状态，仅类内部可写；外部只能读 holder.uiState
    var uiState by mutableStateOf(KnowledgeUiState())
        private set

    private var webView: WebView? = null

    /** 取出已创建的 WebView */
    fun peek(): WebView? = webView

    /** 取 WebView，首次创建并加载知识库首页 */
    fun obtain(context: Context): WebView = webView ?: createWebView(context).also {
        webView = it
        it.loadUrl(Constants.KNOWLEDGE_BASE_URL)
    }

    /** 重试加载 */
    fun retry() {
        uiState = uiState.copy(errorMessage = null)
        webView?.reload()
    }

    /** 浏览器打开 */
    fun openInBrowser(context: Context) {
        UrlRouter.openExternally(context, Constants.KNOWLEDGE_BASE_URL.toUri())
    }

    /** 销毁 */
    fun destroy() {
        webView?.let { wv ->
            // 从 View 树中移除 WebView
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.destroy()
        }
        webView = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView {
        // 只在 debug 包放开 chrome://inspect 远程调试
        WebView.setWebContentsDebuggingEnabled(
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        )

        // 打开第三方 Cookie
        CookieManager.getInstance().setAcceptCookie(true)

        val wv = WebView(context)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = true
            // 加载的是外部网页，禁掉 file:// 和 content:// 访问，防止本地文件泄露
            allowFileAccess = false
            allowContentAccess = false
            // 去掉 WebView 默认 UA 里的 "; wv" 标记
            // 防止请求被判定成非标准浏览器环境的降级或拦截
            userAgentString = userAgentString.replace("; wv", "")
        }

        // 绑定客户端
        wv.webChromeClient = knowledgeChromeClient()
        wv.webViewClient = knowledgeWebViewClient()

        // 系统浏览器接管下载
        wv.setDownloadListener { url, _, _, _, _ ->
            UrlRouter.openExternally(wv.context, Uri.parse(url))
        }

        return wv
    }

    /** ChromeClient 实现 */
    private fun knowledgeChromeClient() = object : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            uiState = uiState.copy(progress = newProgress)
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message
        ): Boolean {
            val probe = WebView(view.context)// 探针 WebView
            probe.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    probeView: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val uri = request.url
                    // 判断域名是否属于站内
                    if (UrlRouter.isInternalHost(uri.host)) {
                        view.loadUrl(uri.toString())
                    } else {
                        UrlRouter.openExternally(view.context, uri)
                    }
                    probeView.destroy()
                    return true
                }
            }
            val transport = resultMsg.obj as? WebView.WebViewTransport
            transport?.webView = probe
            resultMsg.sendToTarget()
            return true
        }
    }

    /** WebViewClient 实现 */
    private fun knowledgeWebViewClient() = object : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            uiState = uiState.copy(loading = true, errorMessage = null, canGoBack = view.canGoBack())
        }

        override fun onPageFinished(view: WebView, url: String?) {
            uiState = uiState.copy(loading = false, canGoBack = view.canGoBack())
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            uiState = uiState.copy(canGoBack = view.canGoBack())
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            // 子资源失败不遮挡页面
            if (!request.isForMainFrame) return
            val message = error.description?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: "页面加载失败"
            uiState = uiState.copy(loading = false, errorMessage = message)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val uri = request.url
            if (UrlRouter.isInternalHost(uri.host)) return false// 站内跳转
            UrlRouter.openExternally(view.context, uri)
            return true
        }
    }
}
