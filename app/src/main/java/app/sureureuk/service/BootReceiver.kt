package app.sureureuk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog

/**
 * 재부팅 시 지난 타이머를 정리한다.
 * 볼륨·화면 꺼짐 시간 복원은 이 수신기로 프로세스가 뜰 때 SureureukApp.onCreate에서 처리된다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (prefs.endAtMillis > 0) {
            VerifyLog.i("부팅", "재부팅으로 진행 중이던 타이머 정리")
            prefs.endAtMillis = 0
        }
    }
}
