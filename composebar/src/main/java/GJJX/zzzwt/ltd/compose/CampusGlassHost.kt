package GJJX.zzzwt.ltd.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 演示宿主：管理 About 对话框状态，承载纯 Compose 玻璃演示页。
 * 无 WebView、无 AndroidView —— backdrop 录制范围全部是 Compose 内容，
 * 与 KSU 本尊（MainActivity.kt L388 layerBackdrop 挂法）同构，玻璃全开。
 */
@Composable
fun CampusGlassHost(
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    blurEnabled: Boolean,
) {
    CampusMiuixTheme {
        var showAbout by remember { mutableStateOf(false) }

        DemoGlassPage(
            selectedPageId = selectedPageId,
            onPageSelected = onPageSelected,
            onLongPressBar = { showAbout = true },
        )

        if (showAbout) {
            AboutDialog(onDismiss = { showAbout = false })
        }
    }
}