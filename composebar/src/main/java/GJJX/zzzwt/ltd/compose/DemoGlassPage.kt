package GJJX.zzzwt.ltd.compose

import android.webkit.WebView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import me.weishu.kernelsu.ui.component.FloatingBottomBar
import me.weishu.kernelsu.ui.component.FloatingBottomBarItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BarItem(val id: String, val label: String, val icon: ImageVector)

val CAMPUS_ITEMS = listOf(
    BarItem("home", "首页", CampusIcons.Home),
    BarItem("tools", "助手", CampusIcons.Tools),
    BarItem("forum", "论坛", CampusIcons.Forum),
    BarItem("gallery", "相册", CampusIcons.Gallery),
    BarItem("scores", "数据", CampusIcons.Scores),
)

/**
 * 最终版主页：WebView 站点内容 + KSU 液态玻璃底栏（图标+文字）。
 *
 * 结构与 KSU MainActivity.kt 同构：
 *   - 录制范围（layerBackdrop）= 页面内容（含 WebView 的 AndroidView +
 *     底层波浪背景）；底栏在录制范围外（KSU 的 Scaffold bottomBar 槽位）。
 *     底栏绝不能进录制范围（自录成环 → RenderThread SIGSEGV）。
 *   - 玻璃全开：blur + vibrancy + lens，底栏实时模糊下方网页。
 */
@Composable
fun CampusSitePage(
    webView: WebView,
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    onUserScroll: () -> Unit,
    onLongPressBar: () -> Unit,
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFFEAF1F8))
        drawWaveBackground()
        drawContent()
    }

    // 玻璃的内容源：曲线背景（纯 Compose，无延迟、无割裂）
    // 说明：曾尝试用 PixelCopy 抓 WebView 画面做 backdrop 以实现「透视网页」，
    // 但抓帧是逐帧异步的，滚动时玻璃会滞后、割裂，观感比纯色更差，故放弃。
    // WebView 是独立 ViewLayer，无法像 KSU 那样被 layerBackdrop 直接录制
    // （会 RenderNode 成环 → SIGSEGV），所以玻璃模糊曲线背景是当前稳定解。

    val selectedIndex = remember(selectedPageId) {
        CAMPUS_ITEMS.indexOfFirst { it.id == selectedPageId }.coerceAtLeast(0)
    }

    // 滑动检测：监听 WebView 自身的滚动位置变化（不劫持触摸事件）。
    // 之前用 setOnTouchListener 拦截 ACTION_MOVE，会干扰 WebView 的手势识别，
    // 表现为"下滑之后划不回去"。改成读 onScrollChanged 完全无侵入。
    DisposableEffect(webView) {
        val listener = object : android.view.View.OnScrollChangeListener {
            override fun onScrollChange(
                v: android.view.View,
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int,
            ) {
                if (scrollY != oldScrollY) onUserScroll()
            }
        }
        webView.setOnScrollChangeListener(listener)
        onDispose { webView.setOnScrollChangeListener(null as android.view.View.OnScrollChangeListener?) }
    }

    Box(Modifier.fillMaxSize()) {
        // ==== 录制范围：只有曲线背景（纯 Compose）====
        // 实测结论（2026-10-24）：WebView 不能进 layerBackdrop 录制范围——
        // 每帧录制反复触发 WebView 重排（视觉抽搐）、ViewLayer 录进
        // GraphicsLayer 尺寸错乱（页面缩成 1/20）、最终进程死亡。
        // 因此录制范围只放纯 Compose 波浪背景，玻璃模糊的是它；
        // WebView 叠在其上（透明背景处露出波浪），但在录制范围外。
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            WaveCanvas()
        }

        // ==== WebView：录制范围外，覆盖在波浪背景上 ====
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { },
        )

        // ==== KSU BottomBarMiuix 浮动底栏（必须在录制范围外！）====
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
        FloatingBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
            selectedIndex = selectedIndex,
            onSelected = { index -> onPageSelected(CAMPUS_ITEMS[index].id) },
            backdrop = backdrop,
            tabsCount = CAMPUS_ITEMS.size,
            isBlurEnabled = true,
        ) { activateTab ->
            CAMPUS_ITEMS.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    selected = index == selectedIndex,
                    onClick = { activateTab(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (index == selectedIndex) MiuixTheme.colorScheme.primary
                                   else MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = if (index == selectedIndex) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }
    }
}

/**
 * 曲线背景画布（纯 Compose，进 backdrop 录制范围）。
 */
@Composable
private fun WaveCanvas() {
    androidx.compose.foundation.Canvas(
        Modifier.fillMaxSize()
    ) {
        drawWaveBackground()
    }
}

/**
 * 曲线背景：三层贝塞尔波浪 + 顶部渐变 + 光斑，蓝青配色。
 */
private fun DrawScope.drawWaveBackground() {
    val w = size.width
    val h = size.height

    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFFDCEBFA),
            0.4f to Color(0xFFEAF1F8),
            1f to Color(0xFFF6F9FC),
            startY = 0f,
            endY = h * 0.6f,
        )
    )

    wave(wavePath(Color(0xFFBBD7F2).copy(alpha = 0.55f), h * 0.16f, 130f, w))
    wave(wavePath(Color(0xFF9CC3EC).copy(alpha = 0.5f), h * 0.26f, -110f, w))
    wave(wavePath(Color(0xFF7FB2E3).copy(alpha = 0.45f), h * 0.38f, 90f, w))

    drawCircle(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.5f),
            1f to Color.Transparent,
        ),
        radius = w * 0.35f,
        center = Offset(w * 0.85f, h * 0.08f),
    )
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFCFE4F7).copy(alpha = 0.6f),
            1f to Color.Transparent,
        ),
        radius = w * 0.28f,
        center = Offset(w * 0.12f, h * 0.22f),
    )
}

private fun wavePath(color: Color, baseY: Float, amp: Float, w: Float): Pair<Path, Color> {
    val p = Path()
    p.moveTo(0f, baseY)
    p.cubicTo(w * 0.25f, baseY - amp, w * 0.5f, baseY + amp * 0.6f, w * 0.75f, baseY - amp * 0.4f)
    p.cubicTo(w * 0.9f, baseY - amp * 1.1f, w, baseY - amp * 0.2f, w, baseY)
    p.lineTo(w, 0f)
    p.lineTo(0f, 0f)
    p.close()
    return p to color
}

private fun DrawScope.wave(pair: Pair<Path, Color>) {
    drawPath(pair.first, pair.second)
}