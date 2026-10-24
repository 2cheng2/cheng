package GJJX.zzzwt.ltd

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import GJJX.zzzwt.ltd.compose.CampusGlassHost
import GJJX.zzzwt.ltd.compose.LaunchGate

/**
 * 校园服务最终版 —— WebView 站点（zhy2026.ltd）+ KSU 液态玻璃底栏。
 *
 * 架构要点（2026-10-24 全链路实测结论）：
 *   1. WebView 由 Compose AndroidView 独占挂载。
 *   2. 绝不给 WebView 设 LAYER_TYPE_HARDWARE。
 *   3. 底栏必须在 layerBackdrop 录制范围外（自录成环 → RenderThread SIGSEGV）。
 *   4. 底栏切页通过 evaluateJavascript 调站点自身的 switchPage(pageId)。
 *   5. 站点用 body{height:100dvh} + flex 布局，在 WebView 里 100dvh 解析异常
 *      导致 body 高度塌陷为 0、.page-view 缩成 144px（"只有一小条能滚"）。
 *      → 注入 CSS 强制修正 + 开启站点自带的 app-mode（隐藏站点 header/底栏）。
 */
class MainActivity : ComponentActivity() {

    private var selectedPage by mutableStateOf("home")

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            LaunchGate.onNewLaunch(this)
        }

        enableEdgeToEdge()

        webView = createWebView()

        setContent {
            CampusGlassHost(
                webView = webView!!,
                selectedPageId = selectedPage,
                onPageSelected = { pageId ->
                    selectedPage = pageId
                    webView?.evaluateJavascript(
                        "try{switchPage('$pageId')}catch(e){console.warn(e)}", null)
                },
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val wv = WebView(this)
        wv.setBackgroundColor(Color.WHITE)
        wv.setOverScrollMode(View.OVER_SCROLL_NEVER)
        wv.setLayerType(View.LAYER_TYPE_NONE, null)

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString?.replace("; wv", "")?.replace(" Version/4.0", "")
            textZoom = 100
        }

        wv.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (url.contains("zhy2026.ltd")) {
                    view.evaluateJavascript(FIX_LAYOUT_JS, null)
                }
            }
        }

        // 等 WebView 完成首次布局（尺寸>0）后再加载站点
        var loaded = false
        wv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (!loaded && wv.width > 0 && wv.height > 0) {
                loaded = true
                wv.loadUrl("https://zhy2026.ltd")
            }
        }
        return wv
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        /**
         * 布局修正脚本。
         *
         * 站点问题：body{height:100vh} 在 @media(max-width:767px) 里被改成
         * height:100dvh !important。WebView 的 100dvh 解析不可靠，导致
         * body 实际高度 0 → flex:1 的 .page-view 塌陷成 144px。
         *
         * 修正：
         *   1. 用像素高度钉死 body（由 JS 读真实 innerHeight，不依赖 dvh）。
         *   2. 开启站点自带的 app-mode（隐藏站点 header 与站点自己的底栏，
         *      因为我们用的是原生玻璃底栏）。
         *   3. .page-view 显式高度兜底。
         */
        private val FIX_LAYOUT_JS = """
            (function(){
              var h = window.innerHeight;
              var s = document.getElementById('zhy-fix-layout');
              if (!s) {
                s = document.createElement('style');
                s.id = 'zhy-fix-layout';
                document.head.appendChild(s);
              }
              s.textContent = [
                'html, body { height: ' + h + 'px !important; min-height: ' + h + 'px !important; }',
                'body { display: flex !important; flex-direction: column !important; overflow: hidden !important; }',
                '.page-view.active { height: auto !important; flex: 1 1 auto !important; overflow-y: auto !important; -webkit-overflow-scrolling: touch; }'
              ].join('\n');

              // 开启站点自带的 app-mode：隐藏站点 header 和站点自己的底部导航
              document.body.classList.add('app-mode');

              // 让当前页滚回顶部，避免残留滚动位置
              var av = document.querySelector('.page-view.active');
              if (av) av.scrollTop = 0;
            })();
        """.trimIndent()
    }
}