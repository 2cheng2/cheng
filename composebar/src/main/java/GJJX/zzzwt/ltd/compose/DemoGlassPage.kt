package GJJX.zzzwt.ltd.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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

/**
 * 演示页：KSU 液态玻璃底栏 + 曲线背景。
 *
 * 无 WebView —— backdrop 录制的是纯 Compose 内容，与 KSU 本尊同构，
 * 玻璃管线（blur + vibrancy + lens 折射）可全量开启。
 */
@Composable
fun DemoGlassPage(
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    onLongPressBar: () -> Unit,
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFFEAF1F8))
        drawWaveBackground()
        drawContent()
    }

    Box(
        Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
    ) {
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
            Spacer(Modifier.height(160.dp)) // 给浮动底栏留空间
        }

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
private fun DemoCard(i: Int) {
    val titles = listOf("课程表", "成绩查询", "校园卡", "图书馆", "社团活动", "失物招领")
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(84.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
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
}

/**
 * 曲线背景：三层贝塞尔波浪 + 顶部渐变，蓝青配色。
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

    // 第一层波浪（最浅，最高）
    wave(path(Color(0xFFBBD7F2).copy(alpha = 0.55f), h * 0.16f, 130f, w))
    // 第二层波浪
    wave(path(Color(0xFF9CC3EC).copy(alpha = 0.5f), h * 0.26f, -110f, w))
    // 第三层波浪（最深，最低，加一点青）
    wave(path(Color(0xFF7FB2E3).copy(alpha = 0.45f), h * 0.38f, 90f, w))

    // 大圆装饰（模拟光斑）
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

private fun path(color: Color, baseY: Float, amp: Float, w: Float): Pair<Path, Color> {
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
