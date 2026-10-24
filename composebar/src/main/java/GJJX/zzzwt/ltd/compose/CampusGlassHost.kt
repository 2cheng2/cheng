package GJJX.zzzwt.ltd.compose

import android.content.Context
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
 *   - 累计打开次数每满 4 的倍数，则本次使用期间在用户滑动时强制弹一次 About；
 *   - 本次进程内弹过之后，无论怎么滑都不再弹；
 *   - 只有清掉后台（进程被杀）后重新打开，才进入新一轮计数。
 *
 * 即：开 4 次 → 第 4 次使用中滑动时弹 1 次 → 不清后台永不再弹 →
 *     清后台后再开 3 次（第 5、6、7 次打开）→ 第 7 次使用中滑动时再弹。
 */
object LaunchGate {
    private const val PREFS = "zhy_gate"
    private const val KEY_COUNT = "launch_count"

    /** 冷启动时调用（savedInstanceState == null），累计 +1 */
    fun onNewLaunch(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_COUNT, sp.getInt(KEY_COUNT, 0) + 1).apply()
    }

    /**
     * 本次冷启动是否为"滑动可触发 About"的那次使用：
     * 打开次数是 4 的倍数（4、8、12…）。
     */
    fun isAboutLaunch(context: Context): Boolean {
        val c = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_COUNT, 0)
        return c > 0 && c % 4 == 0
    }
}

/**
 * 演示宿主：管理 About 对话框状态，承载纯 Compose 玻璃演示页。
 *
 * About 弹出逻辑：
 *   - 仅当本次冷启动是"第 4n 次打开"时，用户滑动页面会触发弹窗；
 *   - 进程存活期间最多弹一次（弹过后滑动不再触发）；
 *   - 清后台重开进程后重新计数（计数持久化，跨进程累计）。
 */
@Composable
fun CampusGlassHost(
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
    blurEnabled: Boolean,
) {
    CampusMiuixTheme {
        val context = LocalContext.current
        // remember {} 不带 key：进程存活期间只求值一次，转场/重组不会重复读
        val aboutArmed = remember { LaunchGate.isAboutLaunch(context) }
        var showAbout by remember { mutableStateOf(false) }
        var aboutShownThisProcess by remember { mutableStateOf(false) }

        DemoGlassPage(
            selectedPageId = selectedPageId,
            onPageSelected = onPageSelected,
            // 滑动触发（DemoGlassPage 在用户滚动时回调）
            onUserScroll = {
                if (aboutArmed && !aboutShownThisProcess) {
                    aboutShownThisProcess = true
                    showAbout = true
                }
            },
            // 长按底栏保留为手动入口，同样受闸门限制
            onLongPressBar = {
                if (aboutArmed && !aboutShownThisProcess) {
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