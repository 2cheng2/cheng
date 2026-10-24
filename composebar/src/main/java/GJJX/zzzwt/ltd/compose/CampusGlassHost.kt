package GJJX.zzzwt.ltd.compose

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 启动闸门（累计计数，不重置）：
 *
 * 规则：
 *   - 每次打开 App（冷启动）计数 +1，持久化；
 *   - 安装后第 1 次打开即可弹（首次展示署名），之后每开 2 次
 *     （第 3、5、7… 次打开）的那次使用中，滑动时强制弹一次；
 *   - 本次进程内弹过之后，无论怎么滑都不再弹；
 *   - 清掉后台（进程被杀）后重新打开，继续新一轮计数。
 */
object LaunchGate {
    private const val PREFS = "zhy_gate"
    private const val KEY_COUNT = "launch_count"

    /** 冷启动时调用（savedInstanceState == null），累计 +1 */
    fun onNewLaunch(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_COUNT, sp.getInt(KEY_COUNT, 0) + 1).apply()
    }

    /** 第 1、3、5、7… 次打开（奇数次）允许弹 */
    fun isAboutLaunch(context: Context): Boolean {
        val c = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_COUNT, 0)
        return c > 0 && c % 2 == 1
    }
}

/**
 * 站点宿主：WebView + KSU 液态玻璃底栏。
 *
 * About 弹出逻辑（LaunchGate）：
 *   - 第 1 次打开即武装；之后第 3、5、7… 次打开武装；
 *   - 武装状态下用户滑动 WebView / 长按底栏 → 弹出；
 *   - 进程存活期间最多弹一次；清后台重开继续计数。
 */
@Composable
fun CampusGlassHost(
    webView: WebView,
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
) {
    CampusMiuixTheme {
        val context = LocalContext.current
        val aboutArmed = remember { LaunchGate.isAboutLaunch(context) }
        var showAbout by remember { mutableStateOf(false) }
        var aboutShownThisProcess by remember { mutableStateOf(false) }

        fun tryShowAbout() {
            if (aboutArmed && !aboutShownThisProcess) {
                aboutShownThisProcess = true
                showAbout = true
            }
        }

        CampusSitePage(
            webView = webView,
            selectedPageId = selectedPageId,
            onPageSelected = onPageSelected,
            onUserScroll = { tryShowAbout() },
            onLongPressBar = { tryShowAbout() },
        )

        if (showAbout) {
            AboutDialog(onDismiss = { showAbout = false })
        }
    }
}