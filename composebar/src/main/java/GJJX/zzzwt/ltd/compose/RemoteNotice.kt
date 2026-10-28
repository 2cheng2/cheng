package GJJX.zzzwt.ltd.compose

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 远程通知。
 *
 * 设计要点：**通知源完全独立于 zhy2026.ltd 站点**。
 * 站点由第三方维护、经常关停，通知能力是 App 自身功能，
 * 不能跟着站点一起挂掉。
 *
 * 通知源：GitHub（本仓库）
 *   优先用 GitHub API 拉取 release 的 body 作为公告，
 *   这样发布通知 = 在 GitHub 上发一个 release，不用动服务器。
 *
 *   拉取顺序：
 *     1. https://api.github.com/repos/2cheng2/cheng/releases/latest
 *        （读 body 字段，格式见下）
 *     2. 本地缓存（网络不通时用上次成功的内容）
 *
 * 公告格式（写在 release 的说明里，前几行用 JSON）：
 * ┌────────────────────────────────────┐
 * │ {"id":"2026-10-27-1",              │
 * │  "title":"重要通知",                │
 * │  "message":"服务器维护中…"}          │
 * │                                    │
 * │ （下面是正常的 release 说明，可写别的）│
 * └────────────────────────────────────┘
 *
 * 兼容纯 JSON 文件形式，所以也支持：
 *   https://raw.githubusercontent.com/2cheng2/cheng/main/app-notice.json
 *
 * 字段：
 *   id            唯一 id，同 id 只弹一次（必填）
 *   title         弹窗标题（可选，默认「通知」）
 *   message       弹窗正文（必填）
 *   enabled       false 时不下发（可选）
 *   minVersionCode 低于此 versionCode 不弹（可选）
 *   maxVersionCode 高于此 versionCode 不弹，0=不限（可选）
 */
object RemoteNotice {

    private const val PREFS = "zhy_notice"
    private const val KEY_LAST_SEEN = "last_seen_id"
    private const val KEY_CACHE = "notice_cache"

    /** GitHub 仓库（当前项目仓库） */
    private const val REPO = "2cheng2/cheng"

    /**
     * 通知源，按顺序尝试，任一成功即可。
     *
     * 说明：
     *  - 全部走 GitHub，不依赖任何第三方站点（zhy2026.ltd 属站主，不能用）。
     *  - raw.githubusercontent.com 在国内被墙（实测 HTTP 000），必须走镜像。
     *  - 多个镜像轮询，任一可用即可，抗单点故障。
     */
    private val SOURCES = listOf(
        // 1) GitHub API contents 接口（实测可用、内容实时、不依赖第三方镜像）
        "https://api.github.com/repos/$REPO/contents/app-notice.json",
        // 2) gitproxy 镜像（实测国内可达；已加时间戳参数绕开其缓存）
        "https://api.gitproxy.dev/https://raw.githubusercontent.com/$REPO/main/app-notice.json",
        // 3) GitHub API release（发了 release 时可用）
        "https://api.github.com/repos/$REPO/releases/latest",
        // 4) 备用镜像：ghproxy 系列（不同线路，抗单点）
        "https://mirror.ghproxy.com/https://raw.githubusercontent.com/$REPO/main/app-notice.json",
        // 5) 原始地址放最后（海外可用，国内失败也不影响前面成功的情况）
        "https://raw.githubusercontent.com/$REPO/main/app-notice.json",
    )

    private val pool = Executors.newSingleThreadExecutor()

    data class Notice(val id: String, val title: String, val message: String)

    /**
     * 异步拉取通知。有未读通知时回调 onNotice（主线程）。
     * 网络失败时回退到本地缓存。
     */
    fun check(context: Context, versionCode: Int, onNotice: (Notice) -> Unit) {
        val appContext = context.applicationContext
        pool.execute {
            val sp = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            var payload: String? = null
            for (src in SOURCES) {
                payload = fetchNoticeJson(src)
                if (!payload.isNullOrBlank()) break
            }

            if (!payload.isNullOrBlank()) {
                sp.edit().putString(KEY_CACHE, payload).apply()
            } else {
                payload = sp.getString(KEY_CACHE, null)
            }

            if (payload.isNullOrBlank()) return@execute

            try {
                val obj = JSONObject(payload)
                if (!obj.optBoolean("enabled", true)) return@execute

                val minV = obj.optInt("minVersionCode", 0)
                val maxV = obj.optInt("maxVersionCode", 0)
                if (minV > 0 && versionCode < minV) return@execute
                if (maxV > 0 && versionCode > maxV) return@execute

                val id = obj.optString("id").ifBlank { return@execute }
                val title = obj.optString("title").ifBlank { "通知" }
                val message = obj.optString("message")
                if (message.isBlank()) return@execute

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

    /**
     * 从某个源取出公告 JSON 字符串。
     * 兼容三种响应：
     *   1. GitHub contents API —— 返回的是 {content: "base64...", encoding: "base64"}
     *      需要先解 base64 才能拿到真正的 JSON（这是最容易踩的坑）
     *   2. GitHub Release 对象 —— 取其 body 字段，再从文本里抽 JSON
     *   3. 纯 JSON 文件 —— 直接使用
     */
    private fun fetchNoticeJson(url: String): String? {
        val text = httpGet(url) ?: return null
        if (text.isBlank()) return null

        val trimmed = text.trimStart()
        if (!trimmed.startsWith("{")) return extractFromText(text)

        return try {
            val obj = JSONObject(trimmed)

            // 1) GitHub contents API：content 是 base64，需解码
            if (obj.optString("encoding") == "base64" && obj.has("content")) {
                val b64 = obj.optString("content").replace("\n", "").replace("\r", "")
                val decoded = String(
                    android.util.Base64.decode(b64, android.util.Base64.DEFAULT),
                    Charsets.UTF_8,
                )
                return extractFromText(decoded) ?: decoded.trim()
            }

            // 2) GitHub Release 对象：含 tag_name / html_url，取 body
            if (obj.has("tag_name") || obj.has("html_url")) {
                return extractFromText(obj.optString("body"))
            }

            // 3) 纯公告 JSON
            trimmed
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 从任意文本中抽出开头的 JSON 对象（release 说明可能带其他内容）。
     */
    private fun extractFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val start = text.indexOf('{')
        if (start < 0) return null

        // 花括号配对，找到完整的 JSON 对象
        var depth = 0
        var inStr = false
        var esc = false
        for (i in start until text.length) {
            val c = text[i]
            if (inStr) {
                when {
                    esc -> esc = false
                    c == '\\' -> esc = true
                    c == '"' -> inStr = false
                }
            } else {
                when (c) {
                    '"' -> inStr = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            // 追加时间戳参数绕开反向代理/CDN 缓存。
            // 实测：gitproxy 这类镜像会缓存 raw 内容且无视 Cache-Control，
            // 导致改完 app-notice.json 后 App 仍拿到旧公告。
            // 加唯一查询参数后每次都是新请求，镜像无法命中缓存。
            val bust = if (url.contains('?')) "&_t=" else "?_t="
            val finalUrl = url + bust + System.currentTimeMillis()

            conn = (URL(finalUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
                // GitHub API 要求 UA，不设会被 403
                setRequestProperty("User-Agent", "CampusServiceApp")
                setRequestProperty("Accept", "application/vnd.github+json")
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