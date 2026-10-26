package GJJX.zzzwt.ltd.compose

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 远程通知。
 *
 * 设计要点：**不依赖 zhy2026.ltd**。
 * 校园服务站点经常关停维护，若通知源跟着站点走，站点一挂公告就发不出去。
 * 所以通知源走独立地址，并支持多源回退 + 本地缓存：
 *
 *   1. 依次尝试多个通知源（任一可用即可）
 *   2. 成功拉到的内容写入本地缓存
 *   3. 全部失败时——用缓存里上次成功的通知（仍能补发未读公告）
 *
 * 通知源（按顺序尝试，任一成功即返回）：
 *   - https://zhy2026.ltd/app-notice.json     主源（自建域名）
 *   - 备用源见 ALT_URLS，可自行替换为任意静态托管
 *
 * JSON 格式：
 * {
 *   "id": "2026-10-27-1",       // 唯一 id，同 id 只弹一次
 *   "title": "重要通知",
 *   "message": "服务器将于…",
 *   "enabled": true,            // false 时不下发
 *   "minVersionCode": 0,        // 可选，低于此版本不弹
 *   "maxVersionCode": 0         // 可选，0 = 不限
 * }
 */
object RemoteNotice {

    private const val PREFS = "zhy_notice"
    private const val KEY_LAST_SEEN = "last_seen_id"
    private const val KEY_CACHE = "notice_cache"

    /** 通知源，按优先级尝试。任一可用即可，不依赖站点是否在线。 */
    private val SOURCES = listOf(
        // 主源：自建域名下的静态文件
        "https://zhy2026.ltd/app-notice.json",
        // 备用源：换成你自己的任意静态托管地址即可
        // 例如 GitHub Pages / Gitee / 对象存储 等，和站点完全独立
    )

    private val pool = Executors.newSingleThreadExecutor()

    data class Notice(val id: String, val title: String, val message: String)

    /**
     * 异步拉取通知。有未读通知时回调 onNotice（主线程）。
     * 网络失败时回退到本地缓存，保证站点关闭期间也能补发公告。
     */
    fun check(context: Context, versionCode: Int, onNotice: (Notice) -> Unit) {
        val appContext = context.applicationContext
        pool.execute {
            val sp = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            // 依次尝试各通知源，第一个成功的就用它
            var json: String? = null
            for (src in SOURCES) {
                json = fetch(src)
                if (!json.isNullOrBlank()) break
            }

            if (!json.isNullOrBlank()) {
                // 拉取成功：写入缓存，供网络不通时回退使用
                sp.edit().putString(KEY_CACHE, json).apply()
            } else {
                // 全部失败：回退到上次缓存的公告（站点关闭时也能提示）
                json = sp.getString(KEY_CACHE, null)
            }

            if (json.isNullOrBlank()) return@execute

            try {
                val obj = JSONObject(json)
                if (!obj.optBoolean("enabled", true)) return@execute

                val minV = obj.optInt("minVersionCode", 0)
                val maxV = obj.optInt("maxVersionCode", 0)
                if (minV > 0 && versionCode < minV) return@execute
                if (maxV > 0 && versionCode > maxV) return@execute

                val id = obj.optString("id").ifBlank { return@execute }
                val title = obj.optString("title").ifBlank { "通知" }
                val message = obj.optString("message")
                if (message.isBlank()) return@execute

                // 同一条通知只弹一次
                if (sp.getString(KEY_LAST_SEEN, null) == id) return@execute
                sp.edit().putString(KEY_LAST_SEEN, id).apply()

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onNotice(Notice(id, title, message))
                }
            } catch (_: Throwable) {
                // 解析失败静默忽略
            }
        }
    }

    private fun fetch(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}