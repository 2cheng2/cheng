package GJJX.zzzwt.ltd.compose

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 远程通知。
 *
 * 通知内容放在自家域名下（zhy2026.ltd），不依赖任何第三方托管：
 *     https://zhy2026.ltd/app-notice.json
 *
 * JSON 格式（后端只需返回这个结构）：
 * {
 *   "id": "2026-10-27-1",          // 通知唯一 id，同 id 只弹一次
 *   "title": "重要通知",             // 弹窗标题
 *   "message": "服务器维护…",        // 弹窗正文
 *   "enabled": true,                // false 时不下发
 *   "minVersionCode": 0,            // 低于此版本号不弹（可选）
 *   "maxVersionCode": 0             // 高于此版本号不弹（0 表示不限）
 * }
 *
 * 已读 id 持久化在 SharedPreferences，同一条通知不会重复打扰。
 */
object RemoteNotice {

    private const val PREFS = "zhy_notice"
    private const val KEY_LAST_SEEN = "last_seen_id"

    /** 通知源地址：放在自己的域名下 */
    private const val NOTICE_URL = "https://zhy2026.ltd/app-notice.json"

    private val pool = Executors.newSingleThreadExecutor()

    data class Notice(
        val id: String,
        val title: String,
        val message: String,
    )

    /**
     * 异步拉取通知。有新的未读通知时回调 onNotice（主线程）。
     * 任何异常都静默忽略——通知拉不到不能影响 App 正常使用。
     */
    fun check(
        context: Context,
        versionCode: Int,
        onNotice: (Notice) -> Unit,
    ) {
        val appContext = context.applicationContext
        pool.execute {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(NOTICE_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    // 加时间戳参数，绕过可能的 CDN 缓存，保证能实时更新
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (conn.responseCode != 200) return@execute

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                if (text.isBlank()) return@execute

                val obj = JSONObject(text)
                if (!obj.optBoolean("enabled", true)) return@execute

                // 版本区间过滤（可选）
                val minV = obj.optInt("minVersionCode", 0)
                val maxV = obj.optInt("maxVersionCode", 0)
                if (minV > 0 && versionCode < minV) return@execute
                if (maxV > 0 && versionCode > maxV) return@execute

                val id = obj.optString("id").ifBlank { return@execute }
                val title = obj.optString("title").ifBlank { "通知" }
                val message = obj.optString("message")
                if (message.isBlank()) return@execute

                // 同一条通知只弹一次
                val sp = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                if (sp.getString(KEY_LAST_SEEN, null) == id) return@execute

                sp.edit().putString(KEY_LAST_SEEN, id).apply()

                // 回到主线程回调（Compose 状态更新必须在主线程）
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onNotice(Notice(id, title, message))
                }
            } catch (_: Throwable) {
                // 网络失败/解析失败都不影响主流程
            } finally {
                conn?.disconnect()
            }
        }
    }
}
