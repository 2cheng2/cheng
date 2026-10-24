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

        webView = createWebView() // 注意：不在这里 loadUrl —— 布局前加载会让站点 JS 拿到 0 尺寸

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

        // 诊断探针：页面加载完打印布局信息（发布版可移除）
        wv.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                android.util.Log.w("WVPROBE", "console: ${consoleMessage.message()}")
                return true
            }
        }

        // 关键修复：等 WebView 完成首次布局（尺寸>0）后再加载站点。
        // 之前在 onCreate 立即 loadUrl，站点 JS 计算视口高度时
        // WebView 尺寸还是 0，导致 .page-view 容器全部塌陷
        // （实测 body.clientHeight=0、page-view 高 144px），页面"只有一小条"。
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
}