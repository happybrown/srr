package app.sureureuk.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.sureureuk.R
import app.sureureuk.core.Fmt
import app.sureureuk.core.Haptics
import app.sureureuk.core.Notifications
import app.sureureuk.core.Phase
import app.sureureuk.core.Prefs
import app.sureureuk.core.TimerState
import app.sureureuk.core.TimerUi
import app.sureureuk.core.VerifyLog
import app.sureureuk.media.MediaStopper
import app.sureureuk.media.ShakeDetector
import app.sureureuk.media.VolumeFader
import app.sureureuk.overlay.FadeOverlay
import app.sureureuk.screen.ScreenController
import app.sureureuk.tile.SleepTileService
import app.sureureuk.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 스르륵의 본체. 화면 맨 앞에 넷플릭스·유튜브가 떠 있는 동안 뒤에서 돈다.
 *
 * 백그라운드에서 확실히 동작하게 하는 장치:
 *  - 포그라운드 서비스(specialUse): 프로세스 유지 + Android 15의 오디오 포커스 조건 충족
 *  - 정확한 알람 백업: 기기가 잠들어 delay가 늦어지거나 프로세스가 죽어도 정해진 시간에 다시 깨움
 *  - 마무리 단계에만 partial wake lock
 *  - 바꾼 볼륨·화면 설정은 Prefs에 기록해 두고 어떤 경우에도 복원
 */
class SleepTimerService : Service() {

    companion object {
        private const val TAG = "타이머"
        const val ACTION_START = "app.sureureuk.action.START"
        const val ACTION_EXTEND = "app.sureureuk.action.EXTEND"
        const val ACTION_CANCEL = "app.sureureuk.action.CANCEL"
        const val ACTION_END_NOW = "app.sureureuk.action.END_NOW"
        const val ACTION_FIRE = "app.sureureuk.action.FIRE"
        const val ACTION_TEST = "app.sureureuk.action.TEST"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_MILLIS = "millis"
        const val EXTRA_TEST = "test"
        const val EXTRA_DELAY = "delay"
        const val EXTRA_SOURCE = "source"

        fun intent(ctx: Context, action: String): Intent =
            Intent(ctx, SleepTimerService::class.java).setAction(action)

        fun start(ctx: Context, minutes: Int, source: String) =
            send(ctx, intent(ctx, ACTION_START).putExtra(EXTRA_MINUTES, minutes).putExtra(EXTRA_SOURCE, source))

        fun startMillis(ctx: Context, millis: Long, source: String) =
            send(ctx, intent(ctx, ACTION_START).putExtra(EXTRA_MILLIS, millis).putExtra(EXTRA_SOURCE, source))

        fun extend(ctx: Context, minutes: Int, source: String) =
            send(ctx, intent(ctx, ACTION_EXTEND).putExtra(EXTRA_MINUTES, minutes).putExtra(EXTRA_SOURCE, source))

        fun cancel(ctx: Context, source: String) =
            send(ctx, intent(ctx, ACTION_CANCEL).putExtra(EXTRA_SOURCE, source))

        fun test(ctx: Context, kind: TestKind, delaySec: Int) =
            send(ctx, intent(ctx, ACTION_TEST).putExtra(EXTRA_TEST, kind.name).putExtra(EXTRA_DELAY, delaySec))

        private fun send(ctx: Context, i: Intent) = ContextCompat.startForegroundService(ctx, i)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var prefs: Prefs
    private lateinit var stopper: MediaStopper
    private lateinit var fader: VolumeFader
    private lateinit var screen: ScreenController
    private lateinit var overlay: FadeOverlay
    private lateinit var shake: ShakeDetector

    private var phase = Phase.IDLE
    private var endAt = 0L
    private var totalMinutes = 0
    private var testLabel: String? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        stopper = MediaStopper(this)
        fader = VolumeFader(this, prefs)
        screen = ScreenController(this, prefs)
        overlay = FadeOverlay(this, onExtend = { extend(10, "오버레이") }, onStopNow = { endNow("오버레이") })
        shake = ShakeDetector(this) {
            when (phase) {
                Phase.RUNNING -> extend(5, "흔들기")
                Phase.FADING -> extend(10, "흔들기")
                else -> Unit
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!goForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: "알림"
        when (intent?.action) {
            ACTION_START -> {
                val millis = intent.getLongExtra(EXTRA_MILLIS, 0L).takeIf { it > 0 }
                    ?: (intent.getIntExtra(EXTRA_MINUTES, 30) * 60_000L)
                startTimer(millis, source)
            }
            ACTION_EXTEND -> extend(intent.getIntExtra(EXTRA_MINUTES, 5), source)
            ACTION_CANCEL -> cancelTimer(source)
            ACTION_END_NOW -> endNow(source)
            ACTION_FIRE -> onAlarm()
            ACTION_TEST -> {
                val kind = runCatching { TestKind.valueOf(intent.getStringExtra(EXTRA_TEST) ?: "") }.getOrNull()
                if (kind != null) runTest(kind, intent.getIntExtra(EXTRA_DELAY, 10)) else stopIfIdle()
            }
            else -> resumeFromPrefs("시스템이 서비스 재시작")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        overlay.hide()
        shake.stop()
        releaseWakeLock()
        if (TimerState.value.phase != Phase.IDLE) TimerState.flow.value = TimerUi()
        super.onDestroy()
    }

    // ---------------- 타이머 흐름 ----------------

    private fun startTimer(millis: Long, source: String) {
        job?.cancel()
        if (phase == Phase.FADING) {
            hideFadeUi()
            fader.restoreNow("타이머 다시 설정")
        }
        testLabel = null
        endAt = System.currentTimeMillis() + millis
        totalMinutes = ((millis + 59_999) / 60_000).toInt()
        prefs.endAtMillis = endAt
        prefs.totalMinutes = totalMinutes
        VerifyLog.i(TAG, "▶ 타이머 시작 ($source) · ${millis / 1000}초 · ${Fmt.clock(endAt)} 종료")
        scheduleAlarm()
        startShake()
        runLoop()
    }

    private fun resumeFromPrefs(source: String) {
        if (job?.isActive == true) return
        val saved = prefs.endAtMillis
        if (saved <= 0L) {
            stopIfIdle()
            return
        }
        if (System.currentTimeMillis() > saved + 10 * 60_000L) {
            VerifyLog.i(TAG, "이미 한참 지난 타이머 → 정리 ($source)")
            finishTimer()
            return
        }
        endAt = saved
        totalMinutes = prefs.totalMinutes
        VerifyLog.i(TAG, "타이머 복구 ($source) · ${Fmt.clock(endAt)} 종료")
        scheduleAlarm()
        startShake()
        runLoop()
    }

    private fun onAlarm() {
        VerifyLog.i(TAG, "백업 알람으로 깨어남 (상태=$phase)")
        if (endAt == 0L) {
            resumeFromPrefs("알람")
            return
        }
        // 기기가 잠들어 delay가 늦어진 경우: 바로 마무리 단계로
        if (phase == Phase.RUNNING && System.currentTimeMillis() >= fadeStartAt() - 1_500) {
            VerifyLog.i(TAG, "대기 지연 감지 → 바로 마무리 시작")
            runLoop()
        }
    }

    private fun fadeMs(): Long = if (prefs.fadeEnabled) prefs.fadeSeconds * 1000L else 0L
    private fun fadeStartAt(): Long = endAt - fadeMs()

    private fun runLoop() {
        job?.cancel()
        job = scope.launch {
            setPhase(Phase.RUNNING)
            val wait = fadeStartAt() - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            endSequence()
        }
    }

    /** 볼륨 줄이기 → 재생 정지 → 볼륨 복원 → 화면 끄기 */
    private suspend fun endSequence() {
        acquireWakeLock()
        try {
            val fadeLeft = endAt - System.currentTimeMillis()
            if (prefs.fadeEnabled && fadeLeft > 1_000) {
                setPhase(Phase.FADING)
                VerifyLog.i(TAG, "마지막 ${fadeLeft / 1000}초 · 재생: ${stopper.describe()}")
                showFadeUi(endAt)
                val ticker = scope.launch {
                    while (isActive) {
                        overlay.update(((endAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt())
                        delay(1_000)
                    }
                }
                val result = try {
                    fader.fadeOut(fadeLeft)
                } finally {
                    ticker.cancel()
                }
                if (result == VolumeFader.Result.USER_CHANGED) {
                    hideFadeUi()
                    scope.launch { extend(10, "볼륨 버튼 감지") }
                    return
                }
                val rest = endAt - System.currentTimeMillis()
                if (rest > 0) delay(rest)
            }

            setPhase(Phase.ENDING)
            hideFadeUi()

            if (prefs.stopEnabled) {
                val res = stopper.stopAll()
                if (!res.stopped) {
                    fader.muteNow()
                    VerifyLog.i(TAG, "⚠ 재생이 멈추지 않아 볼륨을 최소로 유지")
                }
            }
            delay(1_500)
            if (!fader.restoreIfPending("재생 정지 확인")) {
                TimerAlarmReceiver.scheduleRestoreCheck(this)
                VerifyLog.i(TAG, "15분마다 재생 여부를 보고 볼륨을 복원할게요")
            }

            if (prefs.screenEnabled) screen.turnOff(prefs.screenMode)
            VerifyLog.i(TAG, "■ 마무리 완료")
        } finally {
            stopper.releaseFocus()
            releaseWakeLock()
        }
        finishTimer()
    }

    private fun extend(minutes: Int, source: String) {
        if (endAt == 0L || phase == Phase.ENDING) {
            VerifyLog.i(TAG, "연장 무시 ($source, 상태=$phase)")
            stopIfIdle()
            return
        }
        val wasFading = phase == Phase.FADING
        job?.cancel()
        if (wasFading) {
            hideFadeUi()
            fader.restoreNow("연장")
        }
        endAt = maxOf(endAt, System.currentTimeMillis()) + minutes * 60_000L
        totalMinutes += minutes
        prefs.endAtMillis = endAt
        prefs.totalMinutes = totalMinutes
        Haptics.tick(this)
        VerifyLog.i(TAG, "${minutes}분 연장 ($source) → ${Fmt.clock(endAt)} 종료")
        scheduleAlarm()
        runLoop()
    }

    private fun cancelTimer(source: String) {
        VerifyLog.i(TAG, "타이머 끄기 ($source)")
        job?.cancel()
        if (phase == Phase.FADING) fader.restoreNow("취소")
        hideFadeUi()
        finishTimer()
    }

    private fun endNow(source: String) {
        if (endAt == 0L) {
            VerifyLog.i(TAG, "지금 끄기: 실행 중인 타이머 없음 ($source)")
            stopIfIdle()
            return
        }
        VerifyLog.i(TAG, "지금 바로 끄기 ($source)")
        job?.cancel()
        endAt = System.currentTimeMillis()
        job = scope.launch { endSequence() }
    }

    private fun finishTimer() {
        endAt = 0L
        totalMinutes = 0
        testLabel = null
        prefs.endAtMillis = 0L
        cancelAlarm()
        shake.stop()
        hideFadeUi()
        phase = Phase.IDLE
        publish()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopIfIdle() {
        if (phase == Phase.IDLE && job?.isActive != true) finishTimer()
    }

    // ---------------- 검증 테스트 ----------------

    private fun runTest(kind: TestKind, delaySec: Int) {
        if (endAt != 0L) {
            VerifyLog.i("검증", "타이머 실행 중에는 테스트할 수 없어요")
            return
        }
        job?.cancel()
        testLabel = kind.label
        job = scope.launch {
            setPhase(Phase.TESTING)
            VerifyLog.i("검증", "▶ [${kind.label}] ${delaySec}초 뒤 실행 · 지금 테스트할 앱으로 가서 재생하세요")
            delay(delaySec * 1000L)
            acquireWakeLock()
            try {
                VerifyLog.i("검증", "실행 · 재생 중=${stopper.isPlaying()} · ${stopper.describe()}")
                when (kind) {
                    TestKind.PAUSE_SESSION -> stopper.testSingle(MediaStopper.Method.SESSION)
                    TestKind.PAUSE_KEY -> stopper.testSingle(MediaStopper.Method.MEDIA_KEY)
                    TestKind.PAUSE_FOCUS -> stopper.testSingle(MediaStopper.Method.AUDIO_FOCUS)
                    TestKind.FADE -> {
                        val r = fader.fadeOut(30_000)
                        VerifyLog.i("검증", "볼륨 줄이기 결과: $r")
                        delay(3_000)
                        fader.restoreNow("테스트 끝")
                    }
                    TestKind.SCREEN -> screen.turnOff(prefs.screenMode)
                    TestKind.OVERLAY -> {
                        val until = System.currentTimeMillis() + 15_000
                        showFadeUi(until)
                        repeat(15) {
                            overlay.update(15 - it)
                            delay(1_000)
                        }
                        hideFadeUi()
                    }
                }
            } finally {
                stopper.releaseFocus()
                releaseWakeLock()
            }
            VerifyLog.i("검증", "■ [${kind.label}] 끝")
            finishTimer()
        }
    }

    // ---------------- 표시 ----------------

    private fun setPhase(p: Phase) {
        phase = p
        publish()
    }

    @SuppressLint("MissingPermission")
    private fun publish() {
        TimerState.flow.value = TimerUi(phase, endAt, totalMinutes, testLabel)
        if (phase != Phase.IDLE && Notifications.canPost(this)) {
            NotificationManagerCompat.from(this).notify(Notifications.ID_TIMER, buildTimerNotification())
        }
        SleepTileService.refresh(this)
    }

    private fun goForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            Notifications.ID_TIMER,
            buildTimerNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        true
    } catch (e: Exception) {
        VerifyLog.i(TAG, "⚠ 포그라운드 서비스 시작 실패: ${e.javaClass.simpleName} ${e.message}")
        false
    }

    private fun svcPi(code: Int, action: String, minutes: Int = 0): PendingIntent {
        val i = intent(this, action).putExtra(EXTRA_MINUTES, minutes).putExtra(EXTRA_SOURCE, "알림")
        return PendingIntent.getForegroundService(this, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildTimerNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(this, Notifications.CH_TIMER)
            .setSmallIcon(R.drawable.ic_moon)
            .setColor(Notifications.ACCENT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        when (phase) {
            Phase.RUNNING, Phase.FADING -> b
                .setContentTitle("${Fmt.clock(endAt)}에 잠들어요")
                .setContentText(if (phase == Phase.FADING) "소리를 서서히 줄이는 중" else "끝나면 소리 줄이기 → 재생 멈추기 → 화면 끄기")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(endAt)
                .setShowWhen(true)
                .addAction(0, "+5분", svcPi(10, ACTION_EXTEND, 5))
                .addAction(0, "+15분", svcPi(11, ACTION_EXTEND, 15))
                .addAction(0, "끄기", svcPi(12, ACTION_CANCEL))
            Phase.TESTING -> b.setContentTitle("검증 테스트").setContentText(testLabel ?: "")
            Phase.ENDING -> b.setContentTitle("잘 자요").setContentText("재생을 멈추고 화면을 끄는 중")
            Phase.IDLE -> b.setContentTitle("스르륵").setContentText("준비 중")
        }
        return b.build()
    }

    private fun showFadeUi(until: Long) {
        val shown = prefs.overlayEnabled && overlay.show()
        if (shown) {
            VerifyLog.i(TAG, "영상 위 오버레이 표시")
        } else {
            showHeadsUp(until)
            VerifyLog.i(TAG, if (prefs.overlayEnabled) "오버레이 권한 없음 → 팝업 알림으로 대체" else "팝업 알림 표시")
        }
    }

    private fun hideFadeUi() {
        overlay.hide()
        NotificationManagerCompat.from(this).cancel(Notifications.ID_FADE)
    }

    @SuppressLint("MissingPermission")
    private fun showHeadsUp(until: Long) {
        if (!Notifications.canPost(this)) {
            VerifyLog.i(TAG, "⚠ 알림 권한이 없어 팝업 알림도 띄울 수 없음")
            return
        }
        val n = NotificationCompat.Builder(this, Notifications.CH_FADE)
            .setSmallIcon(R.drawable.ic_moon)
            .setColor(Notifications.ACCENT)
            .setContentTitle("곧 잠들어요")
            .setContentText("소리를 서서히 줄이고 있어요. 깨어 있다면 +10분")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(until)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .addAction(0, "+10분", svcPi(13, ACTION_EXTEND, 10))
            .addAction(0, "지금 끄기", svcPi(14, ACTION_END_NOW))
            .build()
        NotificationManagerCompat.from(this).notify(Notifications.ID_FADE, n)
    }

    // ---------------- 알람 · wake lock · 흔들기 ----------------

    private fun alarmPi(): PendingIntent = PendingIntent.getBroadcast(
        this, 1,
        Intent(this, TimerAlarmReceiver::class.java).setAction(TimerAlarmReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun scheduleAlarm() {
        val am = getSystemService(AlarmManager::class.java)
        val at = fadeStartAt()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmPi())
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmPi())
            VerifyLog.i(TAG, "백업 알람 ${Fmt.clock(at)} (${if (exact) "정확" else "⚠ 부정확: 알람 권한 없음"})")
        } catch (e: SecurityException) {
            VerifyLog.i(TAG, "⚠ 알람 예약 실패: ${e.message}")
        }
    }

    private fun cancelAlarm() {
        getSystemService(AlarmManager::class.java).cancel(alarmPi())
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sureureuk:end")
            .apply {
                setReferenceCounted(false)
                acquire(12 * 60_000L)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startShake() {
        if (prefs.shakeEnabled) shake.start() else shake.stop()
    }
}
