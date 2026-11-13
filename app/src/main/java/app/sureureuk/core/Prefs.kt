package app.sureureuk.core

import android.content.Context

/** 사용자 설정과, 프로세스가 죽어도 복구해야 하는 실행 상태를 함께 보관한다. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("sureureuk", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var lastMinutes: Int
        get() = sp.getInt("lastMinutes", 30)
        set(v) = sp.edit().putInt("lastMinutes", v).apply()

    var fadeEnabled: Boolean
        get() = sp.getBoolean("fadeEnabled", true)
        set(v) = sp.edit().putBoolean("fadeEnabled", v).apply()

    var fadeSeconds: Int
        get() = sp.getInt("fadeSeconds", 60)
        set(v) = sp.edit().putInt("fadeSeconds", v).apply()

    var stopEnabled: Boolean
        get() = sp.getBoolean("stopEnabled", true)
        set(v) = sp.edit().putBoolean("stopEnabled", v).apply()

    var screenEnabled: Boolean
        get() = sp.getBoolean("screenEnabled", true)
        set(v) = sp.edit().putBoolean("screenEnabled", v).apply()

    var overlayEnabled: Boolean
        get() = sp.getBoolean("overlayEnabled", true)
        set(v) = sp.edit().putBoolean("overlayEnabled", v).apply()

    var shakeEnabled: Boolean
        get() = sp.getBoolean("shakeEnabled", true)
        set(v) = sp.edit().putBoolean("shakeEnabled", v).apply()

    // ---- 실행 상태 (commit으로 즉시 기록) ----

    var endAtMillis: Long
        get() = sp.getLong("endAtMillis", 0L)
        set(v) { sp.edit().putLong("endAtMillis", v).commit() }

    var totalMinutes: Int
        get() = sp.getInt("totalMinutes", 0)
        set(v) { sp.edit().putInt("totalMinutes", v).commit() }

    /** 줄이기 전 원래 볼륨. -1이면 복원할 것 없음 */
    var savedVolume: Int
        get() = sp.getInt("savedVolume", -1)
        set(v) { sp.edit().putInt("savedVolume", v).commit() }

    /** 임시로 바꾸기 전 화면 꺼짐 시간(ms). -1이면 복원할 것 없음 */
    var savedScreenTimeout: Int
        get() = sp.getInt("savedScreenTimeout", -1)
        set(v) { sp.edit().putInt("savedScreenTimeout", v).commit() }

    // ---- 업데이트 확인 (GitHub 배포판) ----

    var updCheckedAt: Long
        get() = sp.getLong("updCheckedAt", 0L)
        set(v) = sp.edit().putLong("updCheckedAt", v).apply()

    var updVersion: String?
        get() = sp.getString("updVersion", null)
        set(v) = sp.edit().putString("updVersion", v).apply()

    var updPageUrl: String?
        get() = sp.getString("updPageUrl", null)
        set(v) = sp.edit().putString("updPageUrl", v).apply()

    var updApkUrl: String?
        get() = sp.getString("updApkUrl", null)
        set(v) = sp.edit().putString("updApkUrl", v).apply()

    var updNotes: String?
        get() = sp.getString("updNotes", null)
        set(v) = sp.edit().putString("updNotes", v).apply()
}
