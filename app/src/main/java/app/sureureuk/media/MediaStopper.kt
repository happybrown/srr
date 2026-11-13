package app.sureureuk.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import app.sureureuk.core.Fmt
import app.sureureuk.core.VerifyLog
import app.sureureuk.service.MediaListenerService
import kotlinx.coroutines.delay

/**
 * 다른 앱의 재생을 멈춘다. 한 가지 방법으로는 모든 앱을 멈출 수 없어 순서대로 시도하고,
 * 매번 실제로 멈췄는지 확인한다.
 *  1) 미디어 세션 pause()  — 알림 접근 권한 필요, 가장 정확
 *  2) 미디어 키 PAUSE      — 권한 불필요, 마지막으로 재생한 앱에만 전달
 *  3) 오디오 포커스 가져오기 — 권한 불필요, Android 15+는 포그라운드 서비스 필수
 */
class MediaStopper(private val ctx: Context) {

    enum class Method(val label: String) {
        SESSION("미디어 세션 pause()"),
        MEDIA_KEY("미디어 키 PAUSE"),
        AUDIO_FOCUS("오디오 포커스 가져오기"),
    }

    data class StopResult(val stopped: Boolean, val method: String)

    private val am = ctx.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null

    fun hasListenerAccess(): Boolean = MediaListenerService.isEnabled(ctx)

    fun sessions(): List<MediaController> {
        if (!hasListenerAccess()) return emptyList()
        return try {
            ctx.getSystemService(MediaSessionManager::class.java)
                .getActiveSessions(MediaListenerService.component(ctx))
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** 기록용: "유튜브:재생, 멜론:일시정지" */
    fun describe(): String {
        if (!hasListenerAccess()) return "알 수 없음(알림 접근 권한 필요)"
        val list = sessions()
        if (list.isEmpty()) return "세션 없음"
        return list.joinToString(", ") { "${Fmt.appName(it.packageName)}:${stateName(it.playbackState?.state)}" }
    }

    /** 화면용: 지금 재생 중인 앱 이름 */
    fun nowPlayingLabel(): String {
        if (!hasListenerAccess()) return "알림 접근 권한이 필요해요"
        val playing = sessions().filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        if (playing.isEmpty()) return if (am.isMusicActive) "다른 앱에서 재생 중이에요" else "재생 중인 앱이 없어요"
        val first = Fmt.appName(playing.first().packageName)
        return if (playing.size == 1) first else "$first 외 ${playing.size - 1}개 앱"
    }

    /** 지금 재생 중인 앱의 패키지 이름 (알림 접근 권한이 있을 때만) */
    fun playingPackages(): List<String> =
        sessions().filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }.map { it.packageName }.distinct()

    fun isMusicActive(): Boolean = am.isMusicActive

    fun isPlaying(): Boolean =
        am.isMusicActive || sessions().any { it.playbackState?.state == PlaybackState.STATE_PLAYING }

    /** 방법 하나를 실행하고 대상 수를 돌려준다. */
    private fun run(method: Method): Int = when (method) {
        Method.SESSION -> {
            val targets = sessions().filter {
                val s = it.playbackState?.state
                s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
            }
            targets.forEach {
                it.transportControls.pause()
                VerifyLog.i(TAG, "  → ${Fmt.appName(it.packageName)}에 pause() 전송")
            }
            targets.size
        }
        Method.MEDIA_KEY -> {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            1
        }
        Method.AUDIO_FOCUS -> if (requestFocus()) 1 else 0
    }

    private suspend fun waitStopped(timeoutMs: Long = 2_500): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (!isPlaying()) return true
            delay(250)
        }
        return !isPlaying()
    }

    suspend fun stopAll(): StopResult {
        VerifyLog.i(TAG, "재생 정지 시작 · isMusicActive=${am.isMusicActive} · ${describe()}")
        if (!isPlaying()) {
            VerifyLog.i(TAG, "재생 중인 앱 없음")
            return StopResult(true, "재생 없음")
        }
        for (m in Method.entries) {
            if (m == Method.SESSION && !hasListenerAccess()) {
                VerifyLog.i(TAG, "미디어 세션: 알림 접근 권한 없어 건너뜀")
                continue
            }
            val n = run(m)
            if (n == 0) {
                VerifyLog.i(TAG, "${m.label}: 대상 없음 또는 실패 → 다음 방법")
                continue
            }
            if (waitStopped()) {
                VerifyLog.i(TAG, "✓ ${m.label}(으)로 정지됨 · ${describe()}")
                return StopResult(true, m.label)
            }
            VerifyLog.i(TAG, "✗ ${m.label} 후에도 재생 중 → 다음 방법")
        }
        VerifyLog.i(TAG, "✗ 모든 방법 실패 · ${describe()}")
        return StopResult(false, "실패")
    }

    /** 검증 화면용: 한 가지 방법만 실행하고 결과를 기록한다. */
    suspend fun testSingle(m: Method) {
        if (!isPlaying()) VerifyLog.i(TAG, "⚠ 실행 시점에 재생 중인 앱이 없음 (결과가 의미 없을 수 있음)")
        if (m == Method.SESSION && !hasListenerAccess()) {
            VerifyLog.i(TAG, "✗ 알림 접근 권한이 없어 미디어 세션 방법을 쓸 수 없음")
            return
        }
        val n = run(m)
        VerifyLog.i(TAG, "${m.label} 실행 (대상 ${n}개)")
        val stopped = waitStopped(3_000)
        VerifyLog.i(TAG, if (stopped) "✓ 정지됨 · ${describe()}" else "✗ 여전히 재생 중 · ${describe()}")
    }

    private fun requestFocus(): Boolean {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = req
        val granted = am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        VerifyLog.i(TAG, "오디오 포커스 요청: ${if (granted) "허용" else "거부(포그라운드 서비스가 아니면 Android 15+에서 거부됨)"}")
        return granted
    }

    fun releaseFocus() {
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun stateName(s: Int?): String = when (s) {
        PlaybackState.STATE_PLAYING -> "재생"
        PlaybackState.STATE_PAUSED -> "일시정지"
        PlaybackState.STATE_STOPPED -> "정지"
        PlaybackState.STATE_BUFFERING -> "버퍼링"
        PlaybackState.STATE_NONE, null -> "없음"
        else -> "상태$s"
    }

    companion object {
        private const val TAG = "정지"
    }
}
