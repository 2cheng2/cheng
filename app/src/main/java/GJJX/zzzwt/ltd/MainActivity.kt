package GJJX.zzzwt.ltd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import GJJX.zzzwt.ltd.compose.CampusGlassHost

/**
 * 校园服务 —— 液态玻璃演示模式（Android 9 ~ 15，minSdk 28）。
 *
 * 当前阶段：纯 Compose 演示页（KSU 液态玻璃底栏 + 曲线背景）。
 * WebView 站点壳暂缓接入：miuix-blur 的 layerBackdrop 录制范围
 * 不能包含 AndroidView（ViewLayer 会被重复挂进渲染树，
 * RenderThread prepareTreeImpl 无限递归 → SIGSEGV，2026-10-24 实测）。
 */
class MainActivity : ComponentActivity() {

    private var selectedPage by mutableStateOf("home")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusGlassHost(
                selectedPageId = selectedPage,
                onPageSelected = { selectedPage = it },
                blurEnabled = true,
            )
        }
    }
}