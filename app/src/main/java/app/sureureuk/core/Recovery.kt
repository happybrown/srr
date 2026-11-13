package app.sureureuk.core

import android.content.Context
import app.sureureuk.media.VolumeFader
import app.sureureuk.screen.ScreenController
import app.sureureuk.service.TimerAlarmReceiver

/** 앱이 중간에 종료돼도 사용자 기기 설정(볼륨, 화면 꺼짐 시간)이 바뀐 채로 남지 않게 한다. */
object Recovery {
    fun restorePending(ctx: Context, reason: String) {
        val prefs = Prefs(ctx)
        if (prefs.savedScreenTimeout >= 0) ScreenController(ctx, prefs).restoreTimeout()
        if (prefs.savedVolume >= 0 && !VolumeFader(ctx, prefs).restoreIfPending(reason)) {
            TimerAlarmReceiver.scheduleRestoreCheck(ctx, 60_000L)
        }
    }
}
