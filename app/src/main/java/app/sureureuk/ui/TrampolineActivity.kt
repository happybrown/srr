package app.sureureuk.ui

import android.app.Activity
import android.os.Bundle
import app.sureureuk.core.VerifyLog
import app.sureureuk.service.SleepTimerService

/**
 * 빠른 설정 타일에서 포그라운드 서비스를 바로 시작하지 못할 때 거쳐 가는 투명 화면.
 * 화면이 앞에 있는 동안은 서비스를 시작할 수 있으므로, 시작 후 바로 닫는다.
 */
class TrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        try {
            if (minutes > 0) SleepTimerService.start(this, minutes, "타일(경유)")
            else SleepTimerService.cancel(this, "타일(경유)")
            VerifyLog.i("타일", "✓ 투명 화면 경유로 실행")
        } catch (e: Exception) {
            VerifyLog.i("타일", "✗ 경유 실행도 실패: ${e.javaClass.simpleName}")
        }
        finish()
    }

    companion object {
        const val EXTRA_MINUTES = "minutes"
    }
}
