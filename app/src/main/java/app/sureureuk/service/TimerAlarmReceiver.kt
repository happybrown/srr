package app.sureureuk.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog
import app.sureureuk.media.VolumeFader

class TimerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "app.sureureuk.alarm.FIRE"
        const val ACTION_RESTORE = "app.sureureuk.alarm.RESTORE"

        /** 정지를 무시하는 앱 때문에 볼륨을 못 돌려놨을 때, 재생이 끝나면 복원하도록 주기적으로 확인 */
        fun scheduleRestoreCheck(ctx: Context, delayMs: Long = 15 * 60_000L) {
            val pi = PendingIntent.getBroadcast(
                ctx, 2,
                Intent(ctx, TimerAlarmReceiver::class.java).setAction(ACTION_RESTORE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            ctx.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, pi)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                VerifyLog.i("알람", "백업 알람 수신 → 서비스 깨우기")
                try {
                    // 정확한 알람(setExactAndAllowWhileIdle)은 백그라운드 포그라운드 서비스 시작 예외에 해당
                    ContextCompat.startForegroundService(context, SleepTimerService.intent(context, SleepTimerService.ACTION_FIRE))
                } catch (e: Exception) {
                    VerifyLog.i("알람", "⚠ 서비스 시작 실패: ${e.javaClass.simpleName}")
                }
            }
            ACTION_RESTORE -> {
                val prefs = Prefs(context)
                if (prefs.savedVolume < 0) return
                if (!VolumeFader(context, prefs).restoreIfPending("주기 확인")) scheduleRestoreCheck(context)
            }
        }
    }
}
