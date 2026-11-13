package app.sureureuk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.BuildConfig
import app.sureureuk.R
import app.sureureuk.core.PermState
import app.sureureuk.core.Perms
import app.sureureuk.core.Prefs

private val FADE_SECONDS = listOf(30, 60, 180, 300)

@Composable
fun SettingsScreen(prefs: Prefs, perms: PermState, onBack: () -> Unit, onPerms: () -> Unit, onVerify: () -> Unit) {
    val ctx = LocalContext.current
    var fade by remember { mutableStateOf(prefs.fadeEnabled) }
    var fadeIdx by remember { mutableStateOf(FADE_SECONDS.indexOf(prefs.fadeSeconds).coerceAtLeast(0)) }
    var stop by remember { mutableStateOf(prefs.stopEnabled) }
    var screen by remember { mutableStateOf(prefs.screenEnabled) }
    var overlay by remember { mutableStateOf(prefs.overlayEnabled) }
    var shake by remember { mutableStateOf(prefs.shakeEnabled) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar("설정", onBack)
        Text("타이머가 끝나면 이 순서로 실행해요".keepAll(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted)

        AppCard {
            StepHeader("1") {
                ToggleRow("소리 서서히 줄이기", "갑자기 소리가 끊겨 깨지 않도록", fade) { fade = it; prefs.fadeEnabled = it }
            }
            if (fade) {
                Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("줄이는 시간", fontSize = 13.sp, color = C.muted)
                    Segmented(listOf("30초", "1분", "3분", "5분"), fadeIdx) {
                        fadeIdx = it
                        prefs.fadeSeconds = FADE_SECONDS[it]
                    }
                }
            }
        }

        AppCard {
            StepHeader("2") {
                ToggleRow("재생 중인 앱 멈추기", "유튜브·넷플릭스·음악 앱 등", stop) { stop = it; prefs.stopEnabled = it }
            }
            if (stop && !perms.listener) {
                WarningBanner("알림 접근을 허용해야 확실히 멈춰요", "허용하기", onPerms)
            }
        }

        AppCard {
            StepHeader("3") {
                ToggleRow("화면 끄기", "지문·얼굴 인식은 그대로 돼요", screen) { screen = it; prefs.screenEnabled = it }
            }
            if (screen) {
                Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (BuildConfig.A11Y_LOCK) {
                        ToggleRow(
                            "바로 잠그기",
                            if (perms.accessibility) "전원 버튼을 누른 것처럼 바로 꺼져요"
                            else "접근성 설정에서 켜면 전원 버튼처럼 바로 꺼져요",
                            perms.accessibility,
                        ) { Perms.open(ctx, Perms.accessibilityIntents()) }
                    }
                    if (!perms.overlay && !perms.accessibility) {
                        WarningBanner("'다른 앱 위에 표시'를 허용하지 않으면 영상 앱에서 화면이 안 꺼질 수 있어요", "허용하기") {
                            Perms.open(ctx, Perms.overlayIntents(ctx))
                        }
                    }
                    if (!perms.writeSettings && !perms.accessibility) {
                        TipBanner("'시스템 설정 변경'을 허용하면 10초 만에 꺼져요", "허용하기") {
                            Perms.open(ctx, Perms.writeSettingsIntents(ctx))
                        }
                    }
                }
            }
        }

        AppCard {
            Text("더 편하게", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted)
            ToggleRow(
                "영상 위에 남은 시간 표시",
                if (perms.overlay) "소리가 줄어드는 동안 +10분 버튼을 띄워요" else "권한이 없으면 팝업 알림으로 대신해요",
                overlay,
            ) {
                overlay = it
                prefs.overlayEnabled = it
                if (it && !perms.overlay) Perms.open(ctx, Perms.overlayIntents(ctx))
            }
            ToggleRow("흔들어서 연장", "실행 중엔 +5분, 소리가 줄어드는 중엔 +10분", shake) { shake = it; prefs.shakeEnabled = it }
        }

        AppInfoCard()

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.surface)
                .clickable(role = Role.Button, onClick = onVerify).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("기능 점검", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.text)
                Text("앱마다 재생 멈추기·화면 끄기가 되는지 시험하고 기록을 봐요".keepAll(), fontSize = 12.sp, color = C.muted)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = C.muted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StepHeader(n: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberBadge(n)
        Column(Modifier.padding(start = 12.dp)) { content() }
    }
}
