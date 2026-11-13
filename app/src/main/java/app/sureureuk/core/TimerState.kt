package app.sureureuk.core

import kotlinx.coroutines.flow.MutableStateFlow

enum class Phase { IDLE, RUNNING, FADING, ENDING, TESTING }

data class TimerUi(
    val phase: Phase = Phase.IDLE,
    val endAt: Long = 0L,
    val totalMinutes: Int = 0,
    val testLabel: String? = null,
) {
    val active: Boolean get() = phase == Phase.RUNNING || phase == Phase.FADING || phase == Phase.ENDING
}

/** 서비스가 갱신하고 화면·타일이 읽는 현재 상태 (같은 프로세스 안에서만 유효) */
object TimerState {
    val flow = MutableStateFlow(TimerUi())
    val value: TimerUi get() = flow.value
}
