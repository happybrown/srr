package app.sureureuk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.BuildConfig
import app.sureureuk.R
import app.sureureuk.core.PermState
import app.sureureuk.core.Perms
import app.sureureuk.core.Prefs
import app.sureureuk.update.Updates

private val FADE_SECONDS = listOf(30, 60, 180, 300)

/**
 * 설정: 구역 이름 + 묶음 카드 + 아이콘 행.
 *  1) 타이머가 끝나면 차례로 하는 일  2) 편의 기능  3) 앱 (권한, 기능 점검, 업데이트)
 */
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBar("설정", onBack)

        SectionLabel("타이머가 끝나면 차례로")
        GroupCard {
            SwitchRow(R.drawable.ic_volume, "소리 서서히 줄이기", "갑자기 끊겨서 깨지 않게", fade) { fade = it; prefs.fadeEnabled = it }
            if (fade) {
                Column(Modifier.padding(start = ROW_INSET, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Segmented(listOf("30초", "1분", "3분", "5분"), fadeIdx) {
                        fadeIdx = it
                        prefs.fadeSeconds = FADE_SECONDS[it]
                    }
                    // 고른 시간만큼 끝나기 전에 볼륨이 내려가는 모양
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_volume_high), contentDescription = null, tint = C.muted, modifier = Modifier.size(16.dp))
                        FadeCurve(FADE_SECONDS[fadeIdx], Modifier.weight(1f).height(40.dp).padding(horizontal = 8.dp))
                        Icon(painterResource(R.drawable.ic_volume_off), contentDescription = null, tint = C.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            RowDivider()
            SwitchRow(R.drawable.ic_pause, "재생 멈추기", "유튜브·넷플릭스·음악 앱 등", stop) { stop = it; prefs.stopEnabled = it }
            if (stop && !perms.listener) {
                WarningBanner("알림 접근을 허용해야 확실히 멈춰요", "허용하기", ROW_EXTRA, onPerms)
            }
            RowDivider()
            SwitchRow(R.drawable.ic_phone_off, "화면 끄기", "지문·얼굴 인식은 그대로 돼요", screen) { screen = it; prefs.screenEnabled = it }
            if (screen) {
                if (BuildConfig.A11Y_LOCK) {
                    RowDivider()
                    SwitchRow(
                        R.drawable.ic_person, "바로 잠그기",
                        if (perms.accessibility) "전원 버튼처럼 바로 꺼져요" else "접근성에서 켜면 전원 버튼처럼 바로 꺼져요",
                        perms.accessibility,
                    ) { Perms.open(ctx, Perms.accessibilityIntents()) }
                }
                if (!perms.overlay && !perms.accessibility) {
                    WarningBanner("'다른 앱 위에 표시'가 꺼져 있으면 영상 앱에서 화면이 안 꺼질 수 있어요", "허용하기", ROW_EXTRA) {
                        Perms.open(ctx, Perms.overlayIntents(ctx))
                    }
                }
                if (!perms.writeSettings && !perms.accessibility) {
                    TipBanner("'시스템 설정 변경'을 허용하면 10초 만에 꺼져요", "허용하기", ROW_EXTRA) {
                        Perms.open(ctx, Perms.writeSettingsIntents(ctx))
                    }
                }
            }
        }

        SectionLabel("편의 기능")
        GroupCard {
            SwitchRow(
                R.drawable.ic_layers, "영상 위에 남은 시간",
                if (perms.overlay) "소리가 줄어드는 동안 +10분 버튼을 띄워요" else "권한이 없으면 팝업 알림으로 대신해요",
                overlay,
            ) {
                overlay = it
                prefs.overlayEnabled = it
                if (it && !perms.overlay) Perms.open(ctx, Perms.overlayIntents(ctx))
            }
            RowDivider()
            SwitchRow(R.drawable.ic_shake, "흔들어서 연장", "실행 중엔 +5분, 소리 줄이는 중엔 +10분", shake) { shake = it; prefs.shakeEnabled = it }
        }

        SectionLabel("앱")
        GroupCard {
            val granted = listOf(perms.notifications, perms.listener, perms.exactAlarm).count { it }
            ListRow(
                R.drawable.ic_shield, "권한",
                if (perms.requiredOk) "필수 권한을 모두 허용했어요" else "필수 3개 중 ${granted}개만 허용했어요",
                tint = if (perms.requiredOk) C.mint else C.warn,
                onClick = onPerms,
            ) { Chevron() }
            RowDivider()
            ListRow(R.drawable.ic_activity, "기능 점검", "앱마다 잘 멈추고 꺼지는지 시험해요", onClick = onVerify) { Chevron() }
            UpdateRows()
        }

        // 맨 아래: 버전과 개인정보 한 줄
        Column(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoonIcon(20.dp, C.amber)
            Text(
                "스르륵 ${BuildConfig.VERSION_NAME}" + if (BuildConfig.DEBUG) " · 개발용" else "",
                fontSize = 13.sp, color = C.muted,
            )
            Text(
                (if (Updates.enabled) "새 버전 확인 외에는 인터넷을 쓰지 않고, 어떤 정보도 모으지 않아요"
                else "인터넷을 쓰지 않고, 어떤 정보도 모으지 않아요").keepAll(),
                fontSize = 13.sp, color = C.muted, textAlign = TextAlign.Center,
            )
        }
    }
}
