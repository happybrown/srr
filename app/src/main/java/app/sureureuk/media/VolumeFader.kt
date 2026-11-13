package app.sureureuk.media

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import app.sureureuk.core.Prefs
import app.sureureuk.core.VerifyLog
import kotlinx.coroutines.delay

/**
 * 기기 전체 미디어 볼륨을 한 단계씩 낮춘다 (다른 앱의 볼륨을 직접 조절하는 API는 없음).
 * 단계마다 실제 dB 차이를 보고 간격을 정해, 귀에는 일정한 속도로 작아지게 들리도록 한다.
 */
class VolumeFader(ctx: Context, private val prefs: Prefs) {

    enum class Result { DONE, SKIPPED, USER_CHANGED }

    private val am = ctx.getSystemService(AudioManager::class.java)
    private val stream = AudioManager.STREAM_MUSIC

    private fun minIndex(): Int = if (Build.VERSION.SDK_INT >= 28) am.getStreamMinVolume(stream) else 0

    suspend fun fadeOut(durationMs: Long): Result {
        if (am.isVolumeFixed) {
            VerifyLog.i(TAG, "볼륨 고정 기기 → 줄이기 건너뜀")
            return Result.SKIPPED
        }
        val start = am.getStreamVolume(stream)
        val min = minIndex()
        if (start <= min) {
            VerifyLog.i(TAG, "이미 최소 볼륨")
            return Result.DONE
        }
        if (prefs.savedVolume < 0) prefs.savedVolume = start
        val waits = schedule(start, min, durationMs)
        VerifyLog.i(TAG, "볼륨 $start → $min (최대 ${am.getStreamMaxVolume(stream)}단계) · ${durationMs / 1000}초 동안")

        var expected = start
        var level = start - 1
        for (w in waits) {
            delay(w)
            val cur = am.getStreamVolume(stream)
            if (cur != expected) {
                VerifyLog.i(TAG, "사용자가 볼륨을 바꿈 ($expected → $cur) → 깨어 있다고 판단")
                prefs.savedVolume = -1 // 사용자가 고른 볼륨을 존중
                return Result.USER_CHANGED
            }
            setVolume(level)
            // 블루투스 등에서 정확히 그 단계로 안 맞춰질 수 있어 실제 값을 다시 읽는다
            expected = am.getStreamVolume(stream)
            level--
        }
        return Result.DONE
    }

    /** 단계별 대기 시간. dB 차이가 큰 단계일수록 오래 머문다. */
    private fun schedule(start: Int, min: Int, durationMs: Long): List<Long> {
        val n = start - min
        val even = List(n) { durationMs / n }
        if (Build.VERSION.SDK_INT < 28) return even
        val db = try {
            (min..start).map { am.getStreamVolumeDb(stream, it, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) }
        } catch (e: Exception) {
            return even
        }
        val finite = db.filter { it.isFinite() }
        if (finite.size < 2) return even
        val floor = finite.min() - 20f
        val d = db.map { if (it.isFinite()) it else floor } // d[i] = (min + i)단계의 dB
        val gaps = (0 until n).map { k ->
            (d[start - k - min] - d[start - k - 1 - min]).coerceAtLeast(0.5f)
        }
        val total = gaps.sum()
        VerifyLog.i(TAG, "단계별 dB(스피커 기준): " + d.joinToString(" ") { "%.0f".format(it) })
        return gaps.map { (durationMs * (it / total)).toLong() }
    }

    /** 재생이 멈춘 걸 확인했을 때만 원래 볼륨으로 되돌린다. */
    fun restoreIfPending(reason: String): Boolean {
        val saved = prefs.savedVolume
        if (saved < 0) return true
        if (am.isMusicActive) {
            VerifyLog.i(TAG, "아직 재생 중이라 볼륨 복원 보류 ($reason)")
            return false
        }
        setVolume(saved)
        prefs.savedVolume = -1
        VerifyLog.i(TAG, "볼륨 복원 → $saved ($reason)")
        return true
    }

    /** 연장·취소처럼 계속 들으려는 경우: 재생 여부와 관계없이 바로 되돌린다. */
    fun restoreNow(reason: String) {
        val saved = prefs.savedVolume
        if (saved < 0) return
        setVolume(saved)
        prefs.savedVolume = -1
        VerifyLog.i(TAG, "볼륨 즉시 복원 → $saved ($reason)")
    }

    /** 정지 요청을 무시하는 앱에 대한 최후 수단 */
    fun muteNow() {
        val cur = am.getStreamVolume(stream)
        if (cur <= minIndex()) return
        if (prefs.savedVolume < 0) prefs.savedVolume = cur
        setVolume(minIndex())
        VerifyLog.i(TAG, "볼륨을 최소로 ($cur → ${minIndex()})")
    }

    private fun setVolume(index: Int) {
        try {
            am.setStreamVolume(stream, index, 0) // flag 0: 볼륨 창을 띄우지 않음
        } catch (e: SecurityException) {
            VerifyLog.i(TAG, "볼륨 변경 실패: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "볼륨"
    }
}
