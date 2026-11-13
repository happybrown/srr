package app.sureureuk.core

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import app.sureureuk.service.MediaListenerService

data class PermState(
    val notifications: Boolean,
    val listener: Boolean,
    val exactAlarm: Boolean,
    val battery: Boolean,
    val overlay: Boolean,
    val writeSettings: Boolean,
) {
    val requiredOk: Boolean get() = notifications && listener && exactAlarm
}

object Perms {
    fun read(ctx: Context): PermState {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pm = ctx.getSystemService(PowerManager::class.java)
        return PermState(
            notifications = Notifications.canPost(ctx),
            listener = MediaListenerService.isEnabled(ctx),
            exactAlarm = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms(),
            battery = pm.isIgnoringBatteryOptimizations(ctx.packageName),
            overlay = Settings.canDrawOverlays(ctx),
            writeSettings = Settings.System.canWrite(ctx),
        )
    }

    private fun pkgUri(ctx: Context) = Uri.parse("package:${ctx.packageName}")

    fun listenerIntents(ctx: Context): Array<Intent> {
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (Build.VERSION.SDK_INT < 30) return arrayOf(list)
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, MediaListenerService.component(ctx).flattenToString())
        return arrayOf(detail, list)
    }

    fun exactAlarmIntents(ctx: Context): Array<Intent> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri(ctx))) else emptyArray()

    // 플레이 정책상 직접 요청(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) 대신 설정 목록으로 안내
    fun batteryIntents(): Array<Intent> = arrayOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    fun overlayIntents(ctx: Context): Array<Intent> = arrayOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri(ctx)))

    fun writeSettingsIntents(ctx: Context): Array<Intent> = arrayOf(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri(ctx)))

    fun appDetails(ctx: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(ctx))

    /** 제조사마다 설정 화면이 달라 실패할 수 있으므로 차례로 시도하고, 모두 실패하면 앱 정보 화면을 연다. */
    fun open(ctx: Context, intents: Array<Intent>) {
        for (i in intents) {
            try {
                ctx.startActivity(i)
                return
            } catch (_: Exception) {
            }
        }
        try {
            ctx.startActivity(appDetails(ctx))
        } catch (_: Exception) {
        }
    }
}
