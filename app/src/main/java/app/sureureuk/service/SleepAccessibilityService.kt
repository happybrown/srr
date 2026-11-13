package app.sureureuk.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import app.sureureuk.core.VerifyLog

/**
 * '바로 잠그기' 전용 접근성 서비스. 화면 내용은 읽지 않고(이벤트 구독 없음),
 * 타이머가 끝났을 때 전원 버튼을 누른 것처럼 화면을 잠그는 데만 쓴다.
 * 기기 관리자 잠금과 달리 다음 잠금 해제 때 지문·얼굴 인식을 그대로 쓸 수 있다.
 * (github 배포판 매니페스트에만 등록)
 */
class SleepAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        VerifyLog.i("접근성", "바로 잠그기 사용 가능")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: SleepAccessibilityService? = null

        fun isEnabled(ctx: Context): Boolean {
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(ctx, SleepAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        /** 잠금 요청을 보냈으면 true. 서비스가 꺼져 있거나 Android 9 미만이면 false. */
        fun lockScreen(): Boolean {
            if (Build.VERSION.SDK_INT < 28) return false
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        }
    }
}
