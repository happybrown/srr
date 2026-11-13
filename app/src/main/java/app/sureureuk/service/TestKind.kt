package app.sureureuk.service

/** 검증 화면에서 하나씩 실행하는 테스트 */
enum class TestKind(val label: String) {
    PAUSE_SESSION("정지: 미디어 세션"),
    PAUSE_KEY("정지: 미디어 키"),
    PAUSE_FOCUS("정지: 오디오 포커스"),
    FADE("볼륨 줄이기 30초"),
    SCREEN("화면 끄기"),
    OVERLAY("영상 위 표시 15초"),
}
