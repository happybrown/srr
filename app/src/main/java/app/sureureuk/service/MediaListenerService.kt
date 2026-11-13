package app.sureureuk.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

/**
 * 기능 없음. MediaSessionManager.getActiveSessions()로 다른 앱의 미디어 세션에 접근하려면
 * 알림 접근이 허용된 리스너 컴포넌트가 있어야 해서 둔다. 알림 내용은 읽지 않는다.
 */
class MediaListenerService : NotificationListenerService() {
    companion object {
        fun component(ctx: Context) = ComponentName(ctx, MediaListenerService::class.java)

        fun isEnabled(ctx: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
}
