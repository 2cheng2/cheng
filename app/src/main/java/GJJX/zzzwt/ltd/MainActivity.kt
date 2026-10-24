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
         * 站点原始布局（网页版，非 app-mode）：
         *   body { height: 100vh; display: flex; flex-direction: column; }
         *   .header { position: fixed; height: 64px; }
         *   .page-view { flex: 1; overflow-y: auto; padding-top: 64px; }
         *   .app-bottom-nav { display: none; }   // 非 app-mode 下本来就不显示
         *
         * 问题：手机端媒体查询把 body 改成 height: 100dvh !important。
         * WebView 对 dvh 支持不可靠 → body 高度算成 0 →
         * flex:1 的 .page-view 塌成 144px，只有一小条能滚。
         *
         * 修正（保持站点原设计，只补两处）：
         *   1. html/body 用 height:100%（WebView 中 html 高度 = 视口，可靠），
         *      替掉不可靠的 dvh；保留 flex 纵向布局。
         *   2. .page-view 加 min-height:0 —— flex 子项能正确收缩并滚动的必要条件，
         *      缺了它内容会把容器撑开、overflow-y:auto 失效。
         *   3. 底部留白，避免内容被原生玻璃底栏遮住。
         *
         * 不注入 app-mode：保持站点网页版界面（站点头部导航可见），
         * 站点自己的 .app-bottom-nav 在非 app-mode 下是 display:none，自然不显示。
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
                /* 1. 用百分比高度替代 dvh（body 选择器 + 媒体查询同款，靠 id 样式表后加载取胜） */
                'html { height: 100% !important; }',
                'html body, body { height: 100% !important; min-height: 0 !important; display: flex !important; flex-direction: column !important; overflow: hidden !important; }',
                /* 2. flex 子项允许收缩 + 内层滚动（关键：min-height:0） */
                'html body .page-view, .page-view { flex: 1 1 auto !important; min-height: 0 !important; overflow-y: auto !important; -webkit-overflow-scrolling: touch !important; }',
                'html body .page-view.active, .page-view.active { display: block !important; padding-bottom: 96px !important; }',
                /* 3. 站点自己的底部导航不显示（双保险，非 app-mode 下本已 display:none） */
                'html body .app-bottom-nav, .app-bottom-nav { display: none !important; }'
              ].join('\n');

              // 滚回顶部，避免残留滚动位置
              var av = document.querySelector('.page-view.active');
              if (av) av.scrollTop = 0;
            })();
        """.trimIndent()
    }
}