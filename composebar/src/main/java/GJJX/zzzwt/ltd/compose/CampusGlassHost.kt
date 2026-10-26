package GJJX.zzzwt.ltd.compose

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 启动闸门（累计计数，不重置）。
 *
 * 规则：
 *   - 每次打开 App（冷启动）计数 +1，持久化到 SharedPreferences；
 *   - 安装后第 1 次打开就弹（首次展示署名）；
 *   - 之后每打开 2 次弹 1 次：第 1、3、5、7… 次（奇数次）；
 *   - 每次使用（进程存活期间）最多弹一次，关掉后不再反复弹；
 *   - 清掉后台重开进程后继续累计计数。
 */
object LaunchGate {
    private const val PREFS = "zhy_gate"
    private const val KEY_COUNT = "launch_count"

    /** 冷启动时调用（savedInstanceState == null），累计 +1 */
    fun onNewLaunch(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_COUNT, sp.getInt(KEY_COUNT, 0) + 1).apply()
    }

    /** 本次冷启动是否应弹 About（第 1、3、5… 次打开） */
    fun isAboutLaunch(context: Context): Boolean {
        val c = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_COUNT, 0)
        return c > 0 && c % 2 == 1
    }
}

/**
 * 站点宿主：WebView + KSU 液态玻璃底栏。
 *
 * About 弹出逻辑：
 *   - 进入页面后立即弹（无需用户操作），条件由 LaunchGate 判定；
 *   - 每次进程最多弹一次；
 *   - 长按底栏可手动再看（不受闸门限制）。
 */
@Composable
fun CampusGlassHost(
    webView: WebView,
    selectedPageId: String,
    onPageSelected: (String) -> Unit,
) {
    CampusMiuixTheme {
        val context = LocalContext.current
        var showAbout by remember { mutableStateOf(false) }
        var aboutShownThisProcess by remember { mutableStateOf(false) }

        // 远程通知（服务器下发的公告，不更新 App 也能弹）
        var notice by remember { mutableStateOf<RemoteNotice.Notice?>(null) }

        // 进入即判定：命中闸门就自动弹出（强制展示，不依赖滑动/长按）
        LaunchedEffect(Unit) {
            if (!aboutShownThisProcess && LaunchGate.isAboutLaunch(context)) {
                aboutShownThisProcess = true
                showAbout = true
            }
        }

        // 拉取远程通知：有新的就弹（同一条只弹一次）
        LaunchedEffect(Unit) {
            val versionCode = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }.getOrDefault(0)
            RemoteNotice.check(context, versionCode) { n ->
                notice = n
            }
        }

        CampusSitePage(
            webView = webView,
            selectedPageId = selectedPageId,
            onPageSelected = onPageSelected,
            // 滑动不再触发 About（避免打扰），保留参数以兼容调用方
            onUserScroll = { },
            // 长按底栏不再显示作者信息：作者信息只在「每两次打开」时自动弹一次，
            // 这是唯一入口，避免用户随手长按就弹出来。
            onLongPressBar = { },
        )

        if (showAbout) {
            AboutDialog(onDismiss = { showAbout = false })
        }

        // 远程通知弹窗
        notice?.let { n ->
            NoticeDialog(
                title = n.title,
                message = n.message,
                onDismiss = { notice = null },
            )
        }
    }
}