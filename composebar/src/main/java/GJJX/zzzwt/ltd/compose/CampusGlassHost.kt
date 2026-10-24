package GJJX.zzzwt.ltd.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 启动计数闸门：About 对话框每 4 次冷启动才允许弹出一次。
 * 计数持久化（SharedPreferences，每次冷启动 +1）；
 * 进程存活期间弹过一次后不再弹（不清后台不会重复显示），
 * 清后台重开进程后计数继续累计。
 */
object LaunchGate {
    private const val PREFS = "zhy_gate"
    private const val KEY_COUNT = "launch_count"

    /** 冷启动时调用（savedInstanceState == null） */
    fun onNewLaunch(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_COUNT, sp.getInt(KEY_COUNT, 0) + 1).apply()
    }

    /** 本次冷启动是否为"可弹 About"的启动（第 4、8、12… 次） */
    fun isAboutLaunch(context: Context): Boolean {
        val c = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_COUNT, 0)
        return c > 0 && c % 4 == 0
    }
}

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
        val context = LocalContext.current
        val aboutAllowed = remember { LaunchGate.isAboutLaunch(context) }
        var showAbout by remember { mutableStateOf(false) }
        var aboutShownThisProcess by remember { mutableStateOf(false) }

        DemoGlassPage(
            selectedPageId = selectedPageId,
            onPageSelected = onPageSelected,
            onLongPressBar = {
                if (aboutAllowed && !aboutShownThisProcess) {
                    aboutShownThisProcess = true
                    showAbout = true
                }
            },
        )

        if (showAbout) {
            AboutDialog(onDismiss = { showAbout = false })
        }
    }
}