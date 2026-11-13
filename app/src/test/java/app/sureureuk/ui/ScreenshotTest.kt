package app.sureureuk.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.sureureuk.core.PermState
import app.sureureuk.core.Phase
import app.sureureuk.core.Prefs
import app.sureureuk.core.TimerState
import app.sureureuk.core.TimerUi
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 화면을 이미지로 렌더링해 기기 크기·글꼴 크기별로 겹침이 없는지 눈으로 확인한다.
 * ./gradlew recordRoborazziGithubDebug → app/screenshots 폴더에 PNG 저장
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class ScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val allGranted = PermState(
        notifications = true, listener = true, exactAlarm = true, battery = true,
        overlay = true, writeSettings = true, accessibility = true,
    )
    private val noneGranted = PermState(
        notifications = false, listener = false, exactAlarm = false, battery = false,
        overlay = false, writeSettings = false, accessibility = false,
    )

    @After
    fun reset() {
        TimerState.flow.value = TimerUi()
    }

    private fun shoot(name: String, qualifiers: String, fontScale: Float = 1f, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        rule.mainClock.autoAdvance = false // 실행 중 화면의 1초 타이머 같은 무한 루프 때문에
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                SureureukTheme {
                    Surface(color = C.bg, contentColor = C.text, modifier = Modifier.fillMaxSize()) {
                        // 실제 폰처럼 상태 표시줄(24dp)·내비게이션 바(48dp) 자리를 비운다
                        Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 48.dp)) { content() }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        rule.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Composable
    private fun Home(perms: PermState) {
        HomeScreen(Prefs(LocalContext.current), perms, onStart = {}, onSettings = {}, onPerms = {})
    }

    // ---- 타이머 설정 (첫 화면)

    @Test fun setup_galaxy() = shoot("setup_360x780", "w360dp-h780dp-xxhdpi") { Home(allGranted) }

    @Test fun setup_galaxy_bigFont() = shoot("setup_360x780_font130", "w360dp-h780dp-xxhdpi", 1.3f) { Home(allGranted) }

    // 지원 기기: 갤럭시 S8 이후 (가장 작은 화면 = S8, 360x740dp)
    @Test fun setup_s8() = shoot("setup_360x740_s8", "w360dp-h740dp-xxhdpi") { Home(allGranted) }

    @Test fun setup_s8_banner_bigFont() = shoot("setup_360x740_s8_banner_font130", "w360dp-h740dp-xxhdpi", 1.3f) { Home(noneGranted) }

    @Test fun setup_ultra() = shoot("setup_412x915_ultra", "w412dp-h915dp-xxhdpi") { Home(allGranted) }

    @Test fun setup_s24() = shoot("setup_384x832_s24", "w384dp-h832dp-xxhdpi") { Home(allGranted) }

    private fun withMinutes(m: Int, block: () -> Unit) {
        val ctx = RuntimeEnvironment.getApplication()
        Prefs(ctx).lastMinutes = m
        try { block() } finally { Prefs(ctx).lastMinutes = 30 }
    }

    @Test fun setup_90min() = withMinutes(90) { shoot("setup_360x780_90min", "w360dp-h780dp-xxhdpi") { Home(allGranted) } }

    @Test fun setup_7h45() = withMinutes(465) { shoot("setup_360x780_7h45", "w360dp-h780dp-xxhdpi") { Home(allGranted) } }

    @Test fun running_long() {
        TimerState.flow.value = TimerUi(Phase.RUNNING, System.currentTimeMillis() + (3 * 3600 + 12 * 60 + 9) * 1000L, 240)
        shoot("running_360x780_long", "w360dp-h780dp-xxhdpi") { Home(allGranted) }
    }

    @Test fun setup_s8_bigFont_120() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        Prefs(ctx).lastMinutes = 120
        shoot("setup_360x740_s8_font130_120min", "w360dp-h740dp-xxhdpi", 1.3f) { Home(allGranted) }
        Prefs(ctx).lastMinutes = 30
    }

    // ---- 실행 중

    @Test fun running_galaxy() {
        TimerState.flow.value = TimerUi(Phase.RUNNING, System.currentTimeMillis() + (23 * 60 + 41) * 1000L, 30)
        shoot("running_360x780", "w360dp-h780dp-xxhdpi") { Home(allGranted) }
    }

    @Test fun running_s8_bigFont_hours() {
        TimerState.flow.value = TimerUi(Phase.FADING, System.currentTimeMillis() + (83 * 60 + 5) * 1000L, 90)
        shoot("running_360x740_s8_font130", "w360dp-h740dp-xxhdpi", 1.3f) { Home(allGranted) }
    }

    // ---- 설정 · 권한

    @Test fun settings_galaxy() = shoot("settings_360x780", "w360dp-h780dp-xxhdpi") {
        SettingsScreen(Prefs(LocalContext.current), noneGranted, onBack = {}, onPerms = {}, onVerify = {})
    }

    @Test fun permissions_bigFont() = shoot("permissions_360x780_font130", "w360dp-h780dp-xxhdpi", 1.3f) {
        PermissionsScreen(noneGranted, onRefresh = {}, onDone = {})
    }

    @Test fun permissions_partly() = shoot("permissions_360x780_partly", "w360dp-h780dp-xxhdpi") {
        PermissionsScreen(noneGranted.copy(notifications = true, battery = true), onRefresh = {}, onDone = {})
    }

    // ---- 전체 길이 (스크롤 아래쪽까지 한 장에)

    @Test fun full_settings() = shoot("full_settings", "w360dp-h1900dp-xxhdpi") {
        SettingsScreen(Prefs(LocalContext.current), noneGranted, onBack = {}, onPerms = {}, onVerify = {})
    }

    @Test fun full_settings_bigFont() = shoot("full_settings_font130", "w360dp-h2300dp-xxhdpi", 1.3f) {
        SettingsScreen(Prefs(LocalContext.current), allGranted, onBack = {}, onPerms = {}, onVerify = {})
    }

    @Test fun full_permissions() = shoot("full_permissions_font130", "w360dp-h2300dp-xxhdpi", 1.3f) {
        PermissionsScreen(noneGranted.copy(notifications = true), onRefresh = {}, onDone = {})
    }

    @Test fun full_verify() = shoot("full_verify", "w360dp-h1500dp-xxhdpi") {
        VerifyScreen(Prefs(LocalContext.current), allGranted, onBack = {}, onPerms = {})
    }

    @Test fun full_verify_log() {
        app.sureureuk.core.VerifyLog.clear()
        listOf(
            "검증" to "▶ [정지: 미디어 세션] 10초 뒤 실행 · 지금 테스트할 앱으로 가서 재생하세요",
            "정지" to "미디어 세션 pause() 실행 (대상 1개)",
            "정지" to "✓ 정지됨 · 넷플릭스:일시정지",
            "화면" to "취침 화면 표시 (밝기 최저)",
            "화면" to "✗ 70초 동안 화면이 꺼지지 않음",
            "검증" to "■ [화면 끄기] 끝",
        ).forEach { (t, m) -> app.sureureuk.core.VerifyLog.i(t, m) }
        shoot("full_verify_log", "w360dp-h1500dp-xxhdpi") {
            VerifyScreen(Prefs(LocalContext.current), allGranted, onBack = {}, onPerms = {})
        }
        app.sureureuk.core.VerifyLog.clear()
    }

    @Test fun full_verify_bigFont() = shoot("full_verify_font130", "w360dp-h1800dp-xxhdpi", 1.3f) {
        VerifyScreen(Prefs(LocalContext.current), noneGranted, onBack = {}, onPerms = {})
    }
}
