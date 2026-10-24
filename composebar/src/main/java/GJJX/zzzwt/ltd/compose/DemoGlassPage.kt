package GJJX.zzzwt.ltd.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.component.FloatingBottomBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BarItem(val id: String, val label: String)

val CAMPUS_ITEMS = listOf(
    BarItem("home", "首页"),
    BarItem("tools", "助手"),
    BarItem("forum", "论坛"),
    BarItem("gallery", "相册"),
    BarItem("scores", "数据"),
)

/**
 * 演示页：KSU 液态玻璃底栏 + 曲线背景。
 *
 * 结构严格对齐 KSU（MainActivity.kt L383-471）：
 *   - backdrop 录制范围 = 内容区（波浪背景 + 卡片），【不含底栏】
 *   - 底栏在录制范围之外（KSU 里底栏在 Scaffold.bottomBar，同样在 L388 的 Box 外）
 * 若把底栏放进 layerBackdrop 范围，drawBackdrop 采样 backdrop 的同时
 * backdrop 又在录制底栏自身 → 渲染树成环 → RenderThread 栈溢出（实测 SIGSEGV）。
 */
@Composable
fun DemoGlassPage(
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    onLongPressBar: () -> Unit,
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFFEAF1F8))
        drawContent()
    }

    Box(Modifier.fillMaxSize()) {

        // ===== 内容区：录制范围（KSU 对应 L388 的 Box） =====
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            WaveBackground()

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 48.dp))
                Text(
                    "校园服务",
                    Modifier.padding(horizontal = 28.dp),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16324F),
                )
                Text(
                    "液态玻璃 · 演示模式",
                    Modifier.padding(horizontal = 28.dp, vertical = 6.dp),
                    fontSize = 15.sp,
                    color = Color(0xFF6B7A90),
                )
                Spacer(Modifier.height(24.dp))
                repeat(6) { i ->
                    DemoCard(i)
                }
                Spacer(Modifier.height(160.dp))
            }
        }

        // ===== 底栏：录制范围之外（KSU 对应 Scaffold.bottomBar）=====
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)
        ) {
            CampusBottomBar(
                selectedPageId = selectedPageId,
                onPageSelected = onPageSelected,
                backdrop = backdrop,
                blurEnabled = true,
                onLongPress = onLongPressBar,
            )
        }
    }
}

@Composable
fun CampusBottomBar(
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    backdrop: Backdrop,
    blurEnabled: Boolean,
    onLongPress: () -> Unit = {},
) {
    val selectedIndex = remember(selectedPageId) {
        CAMPUS_ITEMS.indexOfFirst { it.id == selectedPageId }.coerceAtLeast(0)
    }

    FloatingBottomBar(
        selectedIndex = selectedIndex,
        onSelected = { index -> onPageSelected(CAMPUS_ITEMS[index].id) },
        backdrop = backdrop,
        tabsCount = CAMPUS_ITEMS.size,
        isBlurEnabled = blurEnabled,
    ) {
        /* 点击由 FloatingBottomBar 内部 semantics(onClick) 处理（KSU L349-353） */
        CAMPUS_ITEMS.forEachIndexed { index, item ->
            val active = index == selectedIndex
            val color = if (active) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .combinedClickable(onClick = {}, onLongClick = onLongPress),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.label, color = color, style = MiuixTheme.textStyles.main)
            }
        }
    }
}

@Composable
private fun WaveBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawWaveBackground() }
    )
}

@Composable
private fun DemoCard(i: Int) {
    val titles = listOf("课程表", "成绩查询", "校园卡", "图书馆", "社团活动", "失物招领")
    Box(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titles[i], fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16324F))
            Spacer(Modifier.height(4.dp))
            Text("演示内容 #${i + 1}", fontSize = 13.sp, color = Color(0xFF6B7A90))
        }
    }
}

/**
 * 曲线背景绘制逻辑（由 rememberLayerBackdrop 的 record 块调用）。
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

    wave(makeWave(Color(0xFFBBD7F2).copy(alpha = 0.55f), h * 0.16f, 130f, w))
    wave(makeWave(Color(0xFF9CC3EC).copy(alpha = 0.5f), h * 0.26f, -110f, w))
    wave(makeWave(Color(0xFF7FB2E3).copy(alpha = 0.45f), h * 0.38f, 90f, w))

    drawCircle(
        Brush.radialGradient(0f to Color.White.copy(alpha = 0.5f), 1f to Color.Transparent),
        radius = w * 0.35f,
        center = Offset(w * 0.85f, h * 0.08f),
    )
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFCFE4F7).copy(alpha = 0.6f), 1f to Color.Transparent),
        radius = w * 0.28f,
        center = Offset(w * 0.12f, h * 0.22f),
    )
}

private fun makeWave(color: Color, baseY: Float, amp: Float, w: Float): Pair<Path, Color> {
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