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
 *   1. WebView 由 Compose AndroidView 独占挂载（child already has a parent）。
 *   2. 绝不给 WebView 设 LAYER_TYPE_HARDWARE（ViewLayer 会干扰 backdrop 录制）。
 *   3. 底栏必须在 layerBackdrop 录制范围外（自录成环 → RenderThread SIGSEGV）。
 *   4. 底栏切页通过 evaluateJavascript 调站点自身的 switchPage(pageId)。
 */
class MainActivity : ComponentActivity() {

    private var selectedPage by mutableStateOf("home")

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 冷启动计数（About 弹出闸门：第 1、3、5… 次打开武装）
        if (savedInstanceState == null) {
            LaunchGate.onNewLaunch(this)
        }

        enableEdgeToEdge()

        webView = createWebView().also { it.loadUrl("https://zhy2026.ltd") }

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
        // 不透明白底：透明 WebView 在硬件渲染下只绘制初始视口，
        // 下方区域是未绘制瓦片（表现为"只有上半截有内容"）
        wv.setBackgroundColor(Color.WHITE)
        wv.setOverScrollMode(View.OVER_SCROLL_NEVER)
        wv.setLayerType(View.LAYER_TYPE_NONE, null) // 见类注释 2

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString?.replace("; wv", "")?.replace(" Version/4.0", "")
            // 关键：useWideViewPort 必须配合 setLoadWithOverviewMode，
            // 且站点 meta viewport 需要生效。若站点 meta 缺失/异常，
            // 强制文本缩放与初始缩放对齐物理屏。
            textZoom = 100
        }

        // 诊断探针：页面加载完打印 Chromium 内部视口信息
        wv.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                android.util.Log.w("WVPROBE", "console: ${consoleMessage.message()}")
                return true
            }
        }
        wv.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(
                    """(function(){
                    var out=[];
                    document.querySelectorAll('*').forEach(function(e){
                      var s=getComputedStyle(e);
                      if((s.overflowY==='auto'||s.overflowY==='scroll')&&e.scrollHeight>e.clientHeight+10){
                        out.push(e.tagName+'.'+(e.className&&e.className.toString().slice(0,40))+' ch='+e.clientHeight+' sh='+e.scrollHeight);
                      }
                    });
                    var kids=Array.from(document.body.children).slice(0,10).map(function(e){
                      var r=e.getBoundingClientRect();
                      return e.tagName+'.'+(e.className&&e.className.toString().slice(0,40))+' top='+Math.round(r.top)+' h='+Math.round(r.height);
                    });
                    return JSON.stringify({bodyCH:document.body.clientHeight,bodySH:document.body.scrollHeight,kids:kids,scrollers:out.slice(0,8)});
                    })()""",
                ) { v -> android.util.Log.w("WVPROBE", "deep: $v") }
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
}