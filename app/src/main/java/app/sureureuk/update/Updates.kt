package app.sureureuk.update

import android.content.Context
import app.sureureuk.BuildConfig
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases에서 최신 버전을 확인한다 (github 배포판에서만 동작).
 * 보내는 정보는 없다: 인증 없이 공개 API의 latest 릴리스만 읽는다.
 */
object Updates {

    data class Release(val version: String, val pageUrl: String, val apkUrl: String?, val notes: String)

    enum class Result { UPDATE, LATEST, FAILED, DISABLED, SKIPPED }

    private const val TAG = "업데이트"
    private const val INTERVAL_MS = 24 * 60 * 60 * 1000L
    private const val APK_SUFFIX = "-github.apk"

    private val _latest = MutableStateFlow<Release?>(null)

    /** 지금 버전보다 새 릴리스가 있으면 값이 있다. */
    val latest: StateFlow<Release?> = _latest

    val enabled: Boolean get() = BuildConfig.UPDATE_CHECK
    val currentVersion: String get() = BuildConfig.VERSION_NAME
    val releasesPage: String get() = "https://github.com/${BuildConfig.UPDATE_REPO}/releases"

    /** 네트워크 없이 마지막으로 확인한 결과만 불러온다. */
    fun loadCached(ctx: Context) {
        if (!enabled) return
        val p = Prefs(ctx)
        val v = p.updVersion ?: return
        _latest.value = if (isNewer(v, currentVersion)) Release(v, p.updPageUrl ?: releasesPage, p.updApkUrl, p.updNotes ?: "") else null
    }

    /** force=false면 하루에 한 번만 실제로 확인한다. */
    suspend fun check(ctx: Context, force: Boolean): Result {
        if (!enabled) return Result.DISABLED
        val prefs = Prefs(ctx)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.updCheckedAt < INTERVAL_MS) {
            loadCached(ctx)
            return Result.SKIPPED
        }
        val rel = try {
            fetchLatest()
        } catch (e: Exception) {
            VerifyLog.i(TAG, "확인 실패: ${e.javaClass.simpleName}")
            return Result.FAILED
        }
        prefs.updCheckedAt = now
        if (rel == null) {
            _latest.value = null
            return Result.LATEST
        }
        prefs.updVersion = rel.version
        prefs.updPageUrl = rel.pageUrl
        prefs.updApkUrl = rel.apkUrl
        prefs.updNotes = rel.notes
        return if (isNewer(rel.version, currentVersion)) {
            _latest.value = rel
            VerifyLog.i(TAG, "새 버전 ${rel.version} 있음 (현재 $currentVersion)")
            Result.UPDATE
        } else {
            _latest.value = null
            Result.LATEST
        }
    }

    private suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
            .openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Sureureuk-Android")
            when (conn.responseCode) {
                200 -> Unit
                404 -> return@withContext null // 아직 릴리스 없음
                else -> throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            var apk: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(APK_SUFFIX)) {
                        apk = a.optString("browser_download_url")
                        break
                    }
                }
            }
            Release(
                version = json.getString("tag_name").removePrefix("v"),
                pageUrl = json.optString("html_url", releasesPage),
                apkUrl = apk,
                notes = json.optString("body", ""),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** "1.2.10" > "1.2.9". '-' 뒤(예: -dev)는 무시한다. */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
