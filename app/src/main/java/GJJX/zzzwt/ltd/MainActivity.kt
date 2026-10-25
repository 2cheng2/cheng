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
        // 不能用 OVER_SCROLL_NEVER：它会让 WebView 在到达边缘后
        // 无法正确回弹/反向滚动（表现为"下滑后划不回去"）。
        wv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS)
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
                if (!url.contains("zhy2026.ltd")) return
                // 只在站点真正的页面生效：
                // Cloudflare 错误页（1033/522 等）结构完全不同，
                // 强制 flex + overflow:hidden 会把它改白屏，必须跳过。
                // 站点页面的判定依据：存在 .page-view 元素。
                view.evaluateJavascript(
                    "(function(){return !!document.querySelector('.page-view');})()"
                ) { hasPageView ->
                    if (hasPageView == "true") {
                        view.evaluateJavascript(FIX_LAYOUT_JS, null)
                    }
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                error: android.webkit.WebResourceError,
            ) {
                super.onReceivedError(view, request, error)
                // 只在主框架失败时提示（子资源失败不影响页面）
                if (!request.isForMainFrame) return
                showErrorPage(view, error.errorCode, error.description?.toString() ?: "")
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (!request.isForMainFrame) return
                val code = errorResponse.statusCode
                // 5xx：Cloudflare 隧道/源站故障（1033/522/530 等）
                // 4xx：一般是权限或路径问题
                if (code >= 500 || code == 404) {
                    // 延迟判定：识别 Cloudflare 错误页特征
                    // （CF 错误页会返回 5xx 但带着自己的错误页面，样式简陋）
                    view.postDelayed({
                        view.evaluateJavascript(
                            """(function(){
                              var t = (document.body && document.body.innerText || '').toLowerCase();
                              var isCf = t.indexOf('cloudflare') >= 0
                                      || t.indexOf('error 1033') >= 0
                                      || t.indexOf('error 530') >= 0
                                      || t.indexOf('error 522') >= 0
                                      || t.indexOf('error 1016') >= 0
                                      || t.indexOf('bad gateway') >= 0
                                      || t.indexOf('origin is unreachable') >= 0;
                              return isCf ? 'cf' : 'other';
                            })()"""
                        ) { kind ->
                            // CF 错误页 → 用我们自己的友好页接管（间距/居中更规整）
                            // 其他 5xx → 同样接管；非错误页保持原样
                            showErrorPage(view, code, "HTTP $code")
                        }
                    }, 600L)
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

    /** 主框架加载失败时显示可读的错误页，避免白屏无提示 */
    private fun showErrorPage(view: WebView, code: Int, desc: String) {
        val reason = when (code) {
            -2 -> "网络连接已断开"
            -6 -> "无法连接到服务器"
            -8, -9 -> "连接超时"
            522, 523, 524 -> "源服务器无响应（$code）"
            1033 -> "Cloudflare 隧道未启动（1033）"
            else -> desc.ifBlank { "错误码 $code" }
        }
        val html = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
            <style>
              html,body{margin:0;padding:0;width:100%;height:100%}
              html{height:100vh}
              body{height:100vh;display:flex;align-items:center;justify-content:center;
                   font-family:-apple-system,"Segoe UI",sans-serif;
                   background:#f4f7fb;color:#16324f;-webkit-text-size-adjust:100%}
              .box{text-align:center;padding:32px 28px;max-width:360px}
              .icon{font-size:48px;margin-bottom:16px;line-height:1}
              h2{margin:0 0 10px;font-size:19px;font-weight:600}
              p{margin:0 0 22px;font-size:14px;color:#6b7a90;line-height:1.6}
              button{padding:11px 28px;font-size:15px;color:#fff;background:#1a5fb4;
                     border:none;border-radius:10px;font-weight:600;
                     box-shadow:0 4px 12px rgba(26,95,180,.25)}
              button:active{transform:scale(.97)}
            </style></head><body>
            <div class="box">
              <div class="icon">📡</div>
              <h2>暂时连不上校园服务</h2>
              <p>${reason}<br>请检查网络，或稍后再试。</p>
              <button onclick="location.replace('https://zhy2026.ltd')">重新加载</button>
            </div>
            <script>
              /* 兜底：某些 WebView 下 100vh 解析不准，用真实 innerHeight 钉死 */
              (function fix(){
                var h = window.innerHeight;
                if (!h || h < 200) { setTimeout(fix, 50); return; }
                document.documentElement.style.height = h + 'px';
                document.body.style.height = h + 'px';
              })();
            </script>
            </body></html>
        """.trimIndent()
        // baseUrl 必须给真实域名：传 null 会让页面运行在 about:blank 下，
        // 视口高度与 CSS vh 单位计算异常，导致 flex 居中失效、内容挤在顶部。
        view.loadDataWithBaseURL("https://zhy2026.ltd/", html, "text/html", "utf-8", null)
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