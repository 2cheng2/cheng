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
            // 允许文档级滚动（某些站点 CSS 会禁用，这里不退让）
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
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
         * 站点原始布局：body{height:100vh / 100dvh} + display:flex，
         * .page-view{flex:1; overflow-y:auto}。
         * 这套"视口定高 + 内层滚动"在 WebView 里 100dvh 解析不可靠，
         * body 高度算成 0 → .page-view 塌成 144px（只有一小条能滚）。
         *
         * 修正思路：彻底放弃"定高 + 内层滚动"，改回最朴素的文档自然流：
         *   - html/body 高度 auto，去掉 flex 与 overflow 限制
         *   - .page-view 不再当滚动容器（height auto、overflow visible）
         *   - 整页由 WebView 原生滚动承载
         * 这样内容会自然撑开文档、WebView 天然支持上下滚动，
         * 也不会出现"只显示一屏"或"翻不动"。
         */
        private val FIX_LAYOUT_JS = """
            (function(){
              var s = document.getElementById('zhy-fix-layout');
              if (!s) {
                s = document.createElement('style');
                s.id = 'zhy-fix-layout';
                document.head.appendChild(s);
              }
              s.textContent = [
                /* 1. 解除视口定高，恢复文档自然流 */
                'html, body { height: auto !important; min-height: 100% !important; overflow: visible !important; }',
                'body { display: block !important; flex-direction: unset !important; }',
                /* 2. .page-view 不再充当滚动容器，让内容撑开文档 */
                '.page-view { height: auto !important; flex: none !important; overflow: visible !important; }',
                '.page-view.active { display: block !important; }',
                /* 3. 给原生玻璃底栏留出空间（站点 app-mode 自带 70px，再加厚一点） */
                '.page-view.active { padding-bottom: 100px !important; }'
              ].join('\n');

              // 开启站点自带的 app-mode：隐藏站点自己的 header / 底部导航
              // （我们用的是原生液态玻璃底栏）
              document.body.classList.add('app-mode');

              // 滚回顶部，避免残留滚动位置
              window.scrollTo(0, 0);
              var av = document.querySelector('.page-view.active');
              if (av) av.scrollTop = 0;
            })();
        """.trimIndent()
    }
}