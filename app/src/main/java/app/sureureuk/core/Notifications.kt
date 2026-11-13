package app.sureureuk.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Notifications {
    const val CH_TIMER = "timer"
    const val CH_FADE = "fade"
    const val ID_TIMER = 1
    const val ID_FADE = 2
    const val ACCENT = 0xFFA99BFF.toInt()

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val timer = NotificationChannel(CH_TIMER, "타이머 진행", NotificationManager.IMPORTANCE_LOW).apply {
            description = "남은 시간과 연장 버튼"
            setShowBadge(false)
        }
        // 오버레이 권한이 없을 때 영상 위에 잠깐 뜨는 팝업 알림. 소리·진동 없음.
        val fade = NotificationChannel(CH_FADE, "잠들기 직전 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "마지막 1분 동안 연장 버튼을 보여줘요"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        nm.createNotificationChannels(listOf(timer, fade))
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
