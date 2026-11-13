package app.sureureuk.screen

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog
import app.sureureuk.overlay.SleepCurtain
import app.sureureuk.service.SleepAccessibilityService
import kotlinx.coroutines.delay

/**
 * 화면 끄기. 지문·얼굴 인식은 어느 방법이든 그대로 쓸 수 있다.
 *  1) 바로 잠그기: 접근성 서비스가 켜져 있으면 전원 버튼처럼 즉시 잠근다.
 *  2) 취침 화면: 검은 창으로 영상 앱을 가려 그 앱의 '화면 켜두기'를 무력화하고 밝기를 최저로.
 *  3) 화면 꺼짐 시간: '시스템 설정 변경' 권한이 있으면 잠시 10초로 줄였다가 꺼지면 복원.
 * 실제로 몇 초 만에 꺼졌는지 기록한다.
 */
class ScreenController(private val ctx: Context, private val prefs: Prefs) {
    private val pm = ctx.getSystemService(PowerManager::class.java)

    private fun currentTimeout(): Int =
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)

    suspend fun turnOff(curtain: SleepCurtain?) {
        if (!pm.isInteractive) {
            VerifyLog.i(TAG, "이미 화면이 꺼져 있음")
            return
        }
        val started = SystemClock.elapsedRealtime()

        // 1) 바로 잠그기
        if (SleepAccessibilityService.lockScreen()) {
            VerifyLog.i(TAG, "바로 잠그기(접근성) 요청")
            if (awaitOff(5_000) { false }) {
                VerifyLog.i(TAG, "✓ ${elapsedSec(started)}초 만에 화면 꺼짐 (바로 잠그기)")
                return
            }
            VerifyLog.i(TAG, "✗ 바로 잠그기 후에도 화면이 켜져 있음 → 취침 화면으로 대체")
        }

        // 2) 취침 화면
        var touched = false
        val shown = curtain?.show { touched = true } ?: false
        VerifyLog.i(
            TAG,
            if (shown) "취침 화면 표시 (밝기 최저)"
            else "⚠ '다른 앱 위에 표시' 권한이 없어 취침 화면 생략 · 앞의 앱이 화면을 켜 두면 꺼지지 않을 수 있음",
        )

        // 3) 화면 꺼짐 시간 단축
        val systemTimeout = if (prefs.savedScreenTimeout >= 0) prefs.savedScreenTimeout else currentTimeout()
        val limitMs: Long = if (Settings.System.canWrite(ctx)) {
            if (prefs.savedScreenTimeout < 0) prefs.savedScreenTimeout = currentTimeout()
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, SHORT_TIMEOUT)
            VerifyLog.i(TAG, "화면 꺼짐 시간 ${systemTimeout / 1000}초 → ${SHORT_TIMEOUT / 1000}초 (임시)")
            SHORT_TIMEOUT + 60_000L
        } else {
            VerifyLog.i(TAG, "시스템 화면 꺼짐 시간(${systemTimeout / 1000}초)에 맡김 · '시스템 설정 변경'을 허용하면 10초로 빨라짐")
            (systemTimeout + 60_000L).coerceAtMost(11 * 60_000L)
        }

        val off = awaitOff(limitMs) { touched }
        curtain?.hide()
        restoreTimeout()
        when {
            off -> VerifyLog.i(TAG, "✓ ${elapsedSec(started)}초 만에 화면 꺼짐")
            touched -> VerifyLog.i(TAG, "화면을 눌러서 취침 화면을 닫음 (깨어 있음)")
            else -> VerifyLog.i(TAG, "✗ ${limitMs / 1000}초 동안 화면이 꺼지지 않음")
        }
    }

    private fun elapsedSec(since: Long) = (SystemClock.elapsedRealtime() - since) / 1000

    private suspend fun awaitOff(limitMs: Long, cancelled: () -> Boolean): Boolean {
        val end = SystemClock.elapsedRealtime() + limitMs
        while (SystemClock.elapsedRealtime() < end) {
            if (!pm.isInteractive) return true
            if (cancelled()) return false
            delay(300)
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
