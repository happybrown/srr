package app.sureureuk.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.sureureuk.core.Perms
import app.sureureuk.core.Prefs
import app.sureureuk.service.SleepTimerService
import app.sureureuk.update.Updates

class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            SureureukTheme {
                AppRoot(
                    tick = resumeTick.intValue,
                    onStart = { minutes ->
                        SleepTimerService.start(this, minutes, "앱")
                        // 사용자가 보던 앱(넷플릭스·유튜브)으로 바로 돌아간다
                        moveTaskToBack(true)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정 화면에서 권한을 허용하고 돌아오면 상태를 다시 읽는다
        resumeTick.intValue += 1
    }
}

private enum class Screen { Home, Settings, Permissions, Verify }

@Composable
private fun AppRoot(tick: Int, onStart: (Int) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var localTick by remember { mutableIntStateOf(0) }
    val perms = remember(tick, localTick) { Perms.read(ctx) }
    var screen by rememberSaveable { mutableStateOf(if (prefs.onboarded) Screen.Home else Screen.Permissions) }
    BackHandler(enabled = screen != Screen.Home) {
        screen = if (screen == Screen.Verify) Screen.Settings else Screen.Home
    }

    // GitHub 배포판: 앱을 열 때 하루 한 번 새 버전 확인 (play 배포판에서는 아무것도 하지 않음)
    LaunchedEffect(Unit) {
        Updates.loadCached(ctx)
        Updates.check(ctx, force = false)
    }

    Surface(color = C.bg, contentColor = C.text, modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    prefs, perms, onStart,
                    onSettings = { screen = Screen.Settings },
                    onPerms = { screen = Screen.Permissions },
                )
                Screen.Settings -> SettingsScreen(
                    prefs, perms,
                    onBack = { screen = Screen.Home },
                    onPerms = { screen = Screen.Permissions },
                    onVerify = { screen = Screen.Verify },
                )
                Screen.Permissions -> PermissionsScreen(
                    perms,
                    onRefresh = { localTick++ },
                    onDone = {
                        prefs.onboarded = true
                        screen = Screen.Home
                    },
                )
                Screen.Verify -> VerifyScreen(prefs, perms, onBack = { screen = Screen.Settings }, onPerms = { screen = Screen.Permissions })
            }
        }
    }
}
