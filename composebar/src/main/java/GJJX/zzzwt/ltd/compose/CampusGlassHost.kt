package GJJX.zzzwt.ltd.compose

import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
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
 * 液态玻璃宿主 —— 数据流与 KSU MainActivity.kt L363-396 一致。
 */
@Composable
fun CampusGlassHost(
    webView: WebView,
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    CampusMiuixTheme {
        val dark = isSystemInDarkTheme()
        val surface = if (dark) Color(0xFF1E293B) else Color(0xFFFCFCFD)

        val backdrop = rememberLayerBackdrop {
            drawRect(surface)
            drawContent()
        }

        Box(modifier.layerBackdrop(backdrop)) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            }

            CampusBottomBar(
                selectedPageId = selectedPageId,
                onPageSelected = onPageSelected,
                backdrop = backdrop,
                blurEnabled = blurEnabled,
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
    modifier: Modifier = Modifier,
) {
    val selectedIndex = remember(selectedPageId) {
        CAMPUS_ITEMS.indexOfFirst { it.id == selectedPageId }.coerceAtLeast(0)
    }

    FloatingBottomBar(
        modifier = modifier,
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
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.label, color = color, style = MiuixTheme.textStyles.main)
            }
        }
    }
}