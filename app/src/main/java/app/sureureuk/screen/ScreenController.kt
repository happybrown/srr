package app.sureureuk.screen

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog
import kotlinx.coroutines.delay

/**
 * 화면 끄기.
 *  - lock: 기기 관리자 lockNow(). 즉시 꺼지지만 다음 잠금 해제 때 PIN 필요.
 *  - natural: '시스템 설정 변경' 권한이 있으면 화면 꺼짐 시간을 잠시 10초로 줄였다가 꺼지면 복원.
 *             권한이 없으면 시스템 꺼짐 시간에 맡긴다.
 * 어느 경우든 실제로 몇 초 만에 꺼졌는지 기록한다. 꺼지지 않으면 앞의 앱이 화면을 켜두고 있다는 뜻.
 */
class ScreenController(private val ctx: Context, private val prefs: Prefs) {
    private val pm = ctx.getSystemService(PowerManager::class.java)
    private val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(ctx, SleepDeviceAdmin::class.java)

    fun adminActive(): Boolean = dpm.isAdminActive(admin)

    private fun currentTimeout(): Int =
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)

    suspend fun turnOff(mode: String) {
        if (!pm.isInteractive) {
            VerifyLog.i(TAG, "이미 화면이 꺼져 있음")
            return
        }
        val started = SystemClock.elapsedRealtime()
        val systemTimeout = if (prefs.savedScreenTimeout >= 0) prefs.savedScreenTimeout else currentTimeout()

        val limitMs: Long = when {
            mode == Prefs.MODE_LOCK && adminActive() -> {
                dpm.lockNow()
                VerifyLog.i(TAG, "즉시 잠금(lockNow) 호출")
                10_000L
            }
            Settings.System.canWrite(ctx) -> {
                if (mode == Prefs.MODE_LOCK) VerifyLog.i(TAG, "기기 관리자 꺼짐 → 자연스럽게 끄기로 대체")
                if (prefs.savedScreenTimeout < 0) prefs.savedScreenTimeout = currentTimeout()
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, SHORT_TIMEOUT)
                VerifyLog.i(TAG, "화면 꺼짐 시간 ${systemTimeout / 1000}초 → ${SHORT_TIMEOUT / 1000}초 (임시)")
                SHORT_TIMEOUT + 60_000L
            }
            else -> {
                VerifyLog.i(TAG, "시스템 화면 꺼짐 시간(${systemTimeout / 1000}초)에 맡김 · '시스템 설정 변경'을 허용하면 더 빨라짐")
                (systemTimeout + 60_000L).coerceAtMost(6 * 60_000L)
            }
        }

        val off = awaitOff(limitMs)
        val secs = (SystemClock.elapsedRealtime() - started) / 1000
        restoreTimeout()
        if (off) {
            VerifyLog.i(TAG, "✓ ${secs}초 만에 화면 꺼짐")
        } else {
            VerifyLog.i(TAG, "✗ ${limitMs / 1000}초 동안 화면이 꺼지지 않음 → 앞에 있는 앱이 화면을 켜두는 중일 가능성")
        }
    }

    private suspend fun awaitOff(limitMs: Long): Boolean {
        val end = SystemClock.elapsedRealtime() + limitMs
        while (SystemClock.elapsedRealtime() < end) {
            if (!pm.isInteractive) return true
            delay(500)
        }
        return !pm.isInteractive
    }

    fun restoreTimeout() {
        val saved = prefs.savedScreenTimeout
        if (saved < 0) return
        if (Settings.System.canWrite(ctx)) {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, saved)
            VerifyLog.i(TAG, "화면 꺼짐 시간 복원 → ${saved / 1000}초")
        }
        prefs.savedScreenTimeout = -1
    }

    companion object {
        private const val TAG = "화면"
        const val SHORT_TIMEOUT = 10_000
    }
}
