package app.sureureuk.core

import java.util.Calendar

object Fmt {
    fun clock(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val h = c.get(Calendar.HOUR_OF_DAY)
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "${if (h < 12) "오전" else "오후"} $h12:${"%02d".format(c.get(Calendar.MINUTE))}"
    }

    fun mmss(ms: Long): String {
        val s = (ms + 999) / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun duration(secs: Int): String = if (secs < 60) "${secs}초" else "${secs / 60}분"

    private val names = mapOf(
        "com.google.android.youtube" to "유튜브",
        "com.google.android.apps.youtube.music" to "유튜브 뮤직",
        "com.netflix.mediaclient" to "넷플릭스",
        "com.spotify.music" to "스포티파이",
        "com.iloen.melon" to "멜론",
        "skplanet.musicmate" to "FLO",
        "com.ktmusic.geniemusic" to "지니",
        "kr.co.captv.pooqV2" to "웨이브",
        "net.cj.cjhv.gs.tving" to "티빙",
        "com.coupang.mobile.play" to "쿠팡플레이",
        "com.frograms.wplay" to "왓챠",
        "com.disney.disneyplus" to "디즈니+",
        "com.sec.android.app.music" to "삼성 뮤직",
    )

    fun appName(pkg: String): String = names[pkg] ?: pkg
}
