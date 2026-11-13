package app.sureureuk

import android.app.Application
import app.sureureuk.core.Notifications
import app.sureureuk.core.Recovery
import app.sureureuk.core.VerifyLog

class SureureukApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VerifyLog.init(this)
        Notifications.createChannels(this)
        // 이전 실행이 중간에 끊겼다면 바꿔둔 볼륨·화면 꺼짐 시간을 되돌린다
        Recovery.restorePending(this, "앱 프로세스 시작")
    }
}
