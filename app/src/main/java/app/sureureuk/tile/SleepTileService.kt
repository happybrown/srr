package app.sureureuk.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import app.sureureuk.R
import app.sureureuk.core.Fmt
import app.sureureuk.core.TimerState
import app.sureureuk.core.VerifyLog
import app.sureureuk.service.SleepTimerService
import app.sureureuk.ui.TrampolineActivity

/**
 * 영상을 보다가 알림창을 내려 바로 시작하는 타일.
 * 누를 때마다 15 → 30 → 60분 → 끄기.
 */
class SleepTileService : TileService() {

    companion object {
        private val PRESETS = listOf(15, 30, 60)

        fun refresh(ctx: Context) {
            try {
                requestListeningState(ctx, ComponentName(ctx, SleepTileService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val ui = TimerState.value
        val next = if (!ui.active) PRESETS.first() else PRESETS.firstOrNull { it > ui.totalMinutes }
        val intent = if (next != null) {
            SleepTimerService.intent(this, SleepTimerService.ACTION_START)
                .putExtra(SleepTimerService.EXTRA_MINUTES, next)
                .putExtra(SleepTimerService.EXTRA_SOURCE, "타일")
        } else {
            SleepTimerService.intent(this, SleepTimerService.ACTION_CANCEL)
                .putExtra(SleepTimerService.EXTRA_SOURCE, "타일")
        }
        val label = next?.let { "${it}분" } ?: "끄기"
        try {
            ContextCompat.startForegroundService(this, intent)
            VerifyLog.i("타일", "✓ 타일에서 바로 실행 ($label)")
        } catch (e: Exception) {
            // 백그라운드에서 포그라운드 서비스 시작이 막힌 경우: 투명 화면을 거쳐 시작
            VerifyLog.i("타일", "✗ 타일에서 바로 실행 실패 (${e.javaClass.simpleName}) → 투명 화면 경유")
            val act = Intent(this, TrampolineActivity::class.java)
                .putExtra(TrampolineActivity.EXTRA_MINUTES, next ?: 0)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchAndCollapse(act)
        }
    }

    private fun launchAndCollapse(act: Intent) {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 3, act, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            legacyCollapse(act)
        }
    }

    // Android 13 이하 전용 (launchAndCollapse에서 SDK 34 미만일 때만 호출)
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun legacyCollapse(act: Intent) {
        startActivityAndCollapse(act)
    }

    private fun render() {
        val tile = qsTile ?: return
        val ui = TimerState.value
        tile.state = if (ui.active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (ui.active) "${Fmt.clock(ui.endAt)} 종료" else "꺼짐"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_moon)
        tile.updateTile()
    }
}
