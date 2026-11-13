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

    @Test fun setup_small() = shoot("setup_360x640", "w360dp-h640dp-xhdpi") { Home(allGranted) }

    @Test fun setup_small_banner_bigFont() = shoot("setup_360x640_banner_font130", "w360dp-h640dp-xhdpi", 1.3f) { Home(noneGranted) }

    @Test fun setup_pixel() = shoot("setup_412x915", "w412dp-h915dp-xxhdpi") { Home(allGranted) }

    @Test fun setup_small_bigFont_120() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        Prefs(ctx).lastMinutes = 120
        shoot("setup_360x640_font130_120min", "w360dp-h640dp-xhdpi", 1.3f) { Home(allGranted) }
        Prefs(ctx).lastMinutes = 30
    }

    // ---- 실행 중

    @Test fun running_galaxy() {
        TimerState.flow.value = TimerUi(Phase.RUNNING, System.currentTimeMillis() + (23 * 60 + 41) * 1000L, 30)
        shoot("running_360x780", "w360dp-h780dp-xxhdpi") { Home(allGranted) }
    }

    @Test fun running_small_bigFont_hours() {
        TimerState.flow.value = TimerUi(Phase.FADING, System.currentTimeMillis() + (83 * 60 + 5) * 1000L, 90)
        shoot("running_360x640_font130", "w360dp-h640dp-xhdpi", 1.3f) { Home(allGranted) }
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
}
