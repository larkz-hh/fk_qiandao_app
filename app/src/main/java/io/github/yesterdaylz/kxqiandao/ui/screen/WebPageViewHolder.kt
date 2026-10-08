package io.github.yesterdaylz.kxqiandao.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.github.yesterdaylz.kxqiandao.util.UrlRouter

data class WebPageUiState(
    val loading: Boolean = true,// 是否正在加载
    val progress: Int = 0,// 进度
    val canGoBack: Boolean = false,// 页面是否能后退
    val errorMessage: String? = null,// 加载失败提示文案
)

/**
 * 内嵌网页的 WebView 持有
 *
 * @param startUrl 首次进入加载的地址
 * @param isInternalHost 站内域名判定，站内跳转留在 WebView，站外交给系统浏览器
 * @param requestGeolocation 页面申请定位时的授权入口
 * @param fixCollapsedLayout 页面根节点高度塌成 0 时用像素高度兜底
 */
class WebPageViewHolder(
    private val startUrl: String,
    private val isInternalHost: (String?) -> Boolean,
    private val requestGeolocation: (onResult: (Boolean) -> Unit) -> Unit = { it(false) },
    private val fixCollapsedLayout: Boolean = false,
) {

    // 页面状态，仅类内部可写；外部只能读 holder.uiState
    var uiState by mutableStateOf(WebPageUiState())
        private set

    private var webView: WebView? = null

    /** 取出已创建的 WebView */
    fun peek(): WebView? = webView

    /** 取 WebView，首次创建并加载首页 */
    fun obtain(context: Context): WebView = webView ?: createWebView(context).also {
        webView = it
        it.loadUrl(startUrl)
    }

    /** 重试加载 */
    fun retry() {
        uiState = uiState.copy(errorMessage = null)
        webView?.reload()
    }

    /** 浏览器打开 */
    fun openInBrowser(context: Context) {
        UrlRouter.openExternally(context, startUrl.toUri())
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
        wv.webChromeClient = pageChromeClient()
        wv.webViewClient = pageWebViewClient()

        // 系统浏览器接管下载
        wv.setDownloadListener { url, _, _, _, _ ->
            UrlRouter.openExternally(wv.context, Uri.parse(url))
        }

        return wv
    }

    /** ChromeClient 实现 */
    private fun pageChromeClient() = object : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            uiState = uiState.copy(progress = newProgress, loading = newProgress < 100)
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            requestGeolocation { granted -> callback.invoke(origin, granted, false) }
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
                    if (isInternalHost(uri.host)) {
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
    private fun pageWebViewClient() = object : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            uiState = uiState.copy(
                loading = true,
                progress = 0,
                errorMessage = null,
                canGoBack = view.canGoBack()
            )
        }

        override fun onPageFinished(view: WebView, url: String?) {
            uiState = uiState.copy(loading = false, canGoBack = view.canGoBack())
            fixCollapsedLayout(view)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            uiState = uiState.copy(canGoBack = view.canGoBack())
            fixCollapsedLayout(view)
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

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            if (!request.isForMainFrame) return
            if (errorResponse.statusCode < 400) return
            uiState = uiState.copy(
                loading = false,
                errorMessage = "HTTP ${errorResponse.statusCode} ${errorResponse.reasonPhrase.orEmpty()}".trim()
            )
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val uri = request.url
            if (isInternalHost(uri.host)) return false// 站内跳转
            UrlRouter.openExternally(view.context, uri)
            return true
        }
    }

    /** 页面里 100vh 恒为 0 导致根节点塌陷，注入像素高度撑开 */
    private fun fixCollapsedLayout(view: WebView) {
        if (!fixCollapsedLayout) return
        view.evaluateJavascript(LAYOUT_FIX_JS) { result ->
            if (result != null && result.contains("1")) {
                Log.w(TAG, "已注入根节点像素高度兜底样式")
            }
        }
    }

    private companion object {
        const val TAG = "WebPageViewHolder"

        val LAYOUT_FIX_JS = """
            (function(){
              function applyRoot(){
                var H=Math.round(window.innerHeight);if(!H)return false;
                var root=document.head||document.documentElement;if(!root)return false;
                var id='__kx_layout_fix__';var st=document.getElementById(id);
                if(!st){st=document.createElement('style');st.id=id;root.appendChild(st)}
                var css='html,body,uni-app{height:'+H+'px !important;}';
                if(st.textContent!==css)st.textContent=css;
                return true;
              }
              function conv(v,H){
                return v.replace(/(-?[\d.]+)vh\b/g,function(_,n){return (parseFloat(n)*H/100).toFixed(2)+'px'});
              }
              function applyVh(){
                var H=Math.round(window.innerHeight);if(!H)return 0;
                var changed=0;
                function walk(rules){
                  for(var i=0;i<rules.length;i++){
                    var r=rules[i];
                    try{
                      if(r.cssRules&&!r.style){walk(r.cssRules);continue}
                      if(r.style){
                        var txt=r.style.cssText||'';
                        if(txt.indexOf('vh')>=0){
                          for(var j=0;j<r.style.length;j++){
                            var p=r.style[j],v=r.style.getPropertyValue(p);
                            if(v&&/(-?[\d.]+)vh/.test(v)){
                              r.style.setProperty(p,conv(v,H),r.style.getPropertyPriority(p));changed++;
                            }
                          }
                        }
                      }
                    }catch(e){}
                  }
                }
                var sheets=[].slice.call(document.styleSheets);
                for(var s=0;s<sheets.length;s++){try{walk(sheets[s].cssRules)}catch(e){}}
                var els=document.querySelectorAll('[style]');
                for(var k=0;k<els.length;k++){
                  var st2=els[k].style;
                  for(var j2=0;j2<st2.length;j2++){
                    var p2=st2[j2],v2=st2.getPropertyValue(p2);
                    if(v2&&/(-?[\d.]+)vh/.test(v2)){
                      st2.setProperty(p2,conv(v2,H),st2.getPropertyPriority(p2));changed++;
                    }
                  }
                }
                return changed;
              }
              applyRoot();
              applyVh();
              setTimeout(applyVh,300);
              setTimeout(applyVh,800);
              setTimeout(applyVh,1600);
              if(!window.__kxLayoutFixBound){
                window.__kxLayoutFixBound=1;
                window.addEventListener('resize',function(){applyRoot();applyVh()});
                setInterval(applyRoot,2000);
              }
              return '1';
            })()
        """.trimIndent()
    }
}
