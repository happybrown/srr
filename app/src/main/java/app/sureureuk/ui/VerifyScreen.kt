package app.sureureuk.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.R
import app.sureureuk.core.PermState
import app.sureureuk.core.Phase
import app.sureureuk.core.Prefs
import app.sureureuk.core.TimerState
import app.sureureuk.core.VerifyLog
import app.sureureuk.media.MediaStopper
import app.sureureuk.service.SleepTimerService
import app.sureureuk.service.TestKind

private const val TEST_DELAY_SEC = 10

private class TestInfo(val icon: Int, val title: String, val desc: String)

private fun TestKind.info(): TestInfo = when (this) {
    TestKind.PAUSE_SESSION -> TestInfo(R.drawable.ic_music, "멈추기 · 미디어 세션", "앱에 직접 요청 (가장 정확)")
    TestKind.PAUSE_KEY -> TestInfo(R.drawable.ic_pause, "멈추기 · 미디어 키", "이어폰 멈춤 버튼처럼")
    TestKind.PAUSE_FOCUS -> TestInfo(R.drawable.ic_volume_high, "멈추기 · 오디오 포커스", "소리 낼 차례를 가져오기")
    TestKind.FADE -> TestInfo(R.drawable.ic_volume, "소리 줄이기", "30초 줄였다가 되돌리기")
    TestKind.SCREEN -> TestInfo(R.drawable.ic_phone_off, "화면 끄기", "바로 잠그기 또는 취침 화면")
    TestKind.OVERLAY -> TestInfo(R.drawable.ic_layers, "영상 위 표시", "15초 동안 띄우기")
}

/**
 * 기능 점검: 실제 기기에서 넷플릭스·유튜브 등을 상대로 각 방법이 통하는지 시험한다.
 * 테스트는 아이콘 행으로, 결과 기록은 성공·실패를 색으로 구분해 보여 준다.
 */
@Composable
fun VerifyScreen(prefs: Prefs, perms: PermState, onBack: () -> Unit, onPerms: () -> Unit) {
    val ctx = LocalContext.current
    val lines by VerifyLog.lines.collectAsState()
    val ui by TimerState.flow.collectAsState()
    val stopper = remember { MediaStopper(ctx) }
    fun sessions(): String = when {
        !stopper.hasListenerAccess() -> "알림 접근 권한이 필요해요"
        else -> stopper.describe().let { if (it == "세션 없음") "재생 중인 앱이 없어요" else it }
    }
    var nowPlaying by remember { mutableStateOf(sessions()) }
    val busy = ui.phase != Phase.IDLE
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBar("기능 점검", onBack)

        // 사용법 한 줄
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.accentSoft).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = C.accentText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "실행을 누르면 ${TEST_DELAY_SEC}초 뒤에 시작해요. 그동안 영상 앱에서 재생해 두세요.".keepAll(),
                fontSize = 14.sp, color = C.text, modifier = Modifier.weight(1f),
            )
        }
        if (!perms.requiredOk) {
            WarningBanner("필수 권한이 꺼져 있어 일부는 실패로 나와요", "허용하기", onClick = onPerms)
        }
        if (busy) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface2).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EqualizerBars(C.accent, Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "${ui.testLabel ?: "타이머"} 진행 중이에요".keepAll(),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.weight(1f),
                )
            }
        }

        SectionLabel("지금 재생 중")
        GroupCard {
            ListRow(R.drawable.ic_music, nowPlaying, tint = C.muted) {
                IconAction(R.drawable.ic_refresh, "새로고침") { nowPlaying = sessions() }
            }
        }

        SectionLabel("하나씩 시험하기")
        GroupCard {
            TestKind.entries.forEachIndexed { i, kind ->
                if (i > 0) RowDivider()
                val t = kind.info()
                ListRow(t.icon, t.title, t.desc) {
                    RunButton("${t.title} 실행", enabled = !busy) { SleepTimerService.test(ctx, kind, TEST_DELAY_SEC) }
                }
            }
        }

        val fullSec = (if (prefs.fadeEnabled) prefs.fadeSeconds else 0) + 20
        PrimaryButton("처음부터 끝까지 시험하기", enabled = !busy, icon = R.drawable.ic_play) {
            SleepTimerService.startMillis(ctx, fullSec * 1000L, "검증")
        }
        Text(
            "${fullSec}초짜리 타이머로 소리 줄이기부터 화면 끄기까지 해 봐요".keepAll(),
            fontSize = 13.sp, color = C.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("기록") {
            IconAction(R.drawable.ic_share, "기록 공유") {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, VerifyLog.export())
                ctx.startActivity(Intent.createChooser(send, "기록 공유"))
            }
            IconAction(R.drawable.ic_trash, "기록 지우기") { VerifyLog.clear() }
        }
        GroupCard {
            if (lines.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_doc), contentDescription = null, tint = C.line, modifier = Modifier.size(32.dp))
                    Text("아직 기록이 없어요", fontSize = 14.sp, color = C.muted)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(lines) { line ->
                        Text(
                            line.withoutDate(), fontSize = 12.sp, lineHeight = 17.sp, fontFamily = FontFamily.Monospace,
                            color = when {
                                '✓' in line -> C.mint
                                '✗' in line || '⚠' in line -> C.warn
                                '▶' in line || '■' in line -> C.accentText
                                else -> C.muted
                            },
                        )
                    }
                }
            }
        }

        Text(
            VerifyLog.deviceInfo().keepAll(),
            fontSize = 13.sp, color = C.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** "11-13 09:12:33 [정지] …" → "09:12:33 [정지] …" (좁은 화면에서 한 줄이라도 덜 꺾이게) */
private fun String.withoutDate(): String =
    if (length > 6 && this[2] == '-' && this[5] == ' ') substring(6) else this
