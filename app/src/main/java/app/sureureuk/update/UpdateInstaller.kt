package app.sureureuk.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog

/**
 * 새 버전 APK를 내려받아 설치 화면을 연다 (github 배포판 전용).
 * 같은 서명 키로 빌드된 APK만 기존 앱 위에 설치된다.
 * 어느 단계든 실패하면 GitHub 릴리스 페이지를 연다.
 */
object UpdateInstaller {
    private const val TAG = "업데이트"
    private const val APK_MIME = "application/vnd.android.package-archive"
    private var receiver: BroadcastReceiver? = null

    fun start(ctx: Context, rel: Updates.Release) {
        val apkUrl = rel.apkUrl
        if (apkUrl.isNullOrBlank()) {
            openPage(ctx, rel.pageUrl)
            return
        }
        // 이 앱이 다른 앱(자기 자신 포함)을 설치하도록 사용자가 허용해야 한다
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(ctx, "'이 출처 허용'을 켠 뒤 다시 눌러 주세요", Toast.LENGTH_LONG).show()
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                openPage(ctx, rel.pageUrl)
            }
            return
        }
        try {
            val dm = ctx.getSystemService(DownloadManager::class.java)
            val req = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("스르륵 ${rel.version}")
                .setDescription("새 버전 내려받는 중")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, "sureureuk-${rel.version}.apk")
            val id = dm.enqueue(req)
            Prefs(ctx).updDownloadId = id
            listen(ctx.applicationContext, id, rel)
            Toast.makeText(ctx, "새 버전을 내려받고 있어요", Toast.LENGTH_SHORT).show()
            VerifyLog.i(TAG, "${rel.version} 다운로드 시작")
        } catch (e: Exception) {
            VerifyLog.i(TAG, "다운로드 시작 실패: ${e.javaClass.simpleName} → 릴리스 페이지 열기")
            openPage(ctx, rel.pageUrl)
        }
    }

    private fun listen(app: Context, id: Long, rel: Updates.Release) {
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != id) return
                runCatching { app.unregisterReceiver(this) }
                receiver = null
                install(app, id, rel)
            }
        }
        receiver = r
        // DownloadManager(시스템)가 보내는 방송이라 EXPORTED로 받아야 한다
        ContextCompat.registerReceiver(app, r, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
    }

    private fun install(app: Context, id: Long, rel: Updates.Release) {
        val dm = app.getSystemService(DownloadManager::class.java)
        val ok = dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        } ?: false
        val uri = if (ok) dm.getUriForDownloadedFile(id) else null
        if (uri == null) {
            VerifyLog.i(TAG, "다운로드 실패 → 릴리스 페이지 열기")
            openPage(app, rel.pageUrl)
            return
        }
        try {
            app.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            VerifyLog.i(TAG, "${rel.version} 설치 화면 열기")
        } catch (e: Exception) {
            // 앱이 뒤로 가 있으면 화면을 띄울 수 없다: 다운로드 완료 알림을 눌러 설치하면 된다
            VerifyLog.i(TAG, "설치 화면을 열지 못함(${e.javaClass.simpleName}) · 다운로드 알림에서 설치 가능")
        }
    }

    fun openPage(ctx: Context, url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
