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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.core.Phase
import app.sureureuk.core.PermState
import app.sureureuk.core.Prefs
import app.sureureuk.core.TimerState
import app.sureureuk.core.VerifyLog
import app.sureureuk.media.MediaStopper
import app.sureureuk.service.SleepTimerService
import app.sureureuk.service.TestKind

private const val TEST_DELAY_SEC = 10

/**
 * 실제 기기에서 넷플릭스·유튜브 등을 상대로 각 방법이 통하는지 확인하는 화면.
 * 결과는 기록에 남고 '공유'로 내보낼 수 있다.
 */
@Composable
fun VerifyScreen(prefs: Prefs, perms: PermState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lines by VerifyLog.lines.collectAsState()
    val ui by TimerState.flow.collectAsState()
    val stopper = remember { MediaStopper(ctx) }
    var nowPlaying by remember { mutableStateOf(stopper.describe()) }
    val busy = ui.phase != Phase.IDLE
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar("기능 검증", onBack)
        Text(VerifyLog.deviceInfo(), fontSize = 13.sp, color = C.muted)
        if (!perms.requiredOk) {
            Text("필수 권한이 꺼져 있으면 일부 테스트 결과가 실패로 나와요.", fontSize = 13.sp, color = C.warn)
        }

        AppCard {
            Text("지금 감지된 미디어 세션", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted)
            Text(nowPlaying, fontSize = 15.sp, color = C.text)
            SecondaryButton("새로고침", height = 44.dp) { nowPlaying = stopper.describe() }
        }

        AppCard {
            Text("${TEST_DELAY_SEC}초 뒤 실행하는 테스트", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Text(
                "버튼을 누른 뒤 ${TEST_DELAY_SEC}초 안에 넷플릭스나 유튜브로 가서 재생하세요. 앱이 뒤에서 실행하고 결과를 아래 기록에 남겨요.",
                fontSize = 13.sp, color = C.muted,
            )
            TestKind.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        SecondaryButton(k.label, Modifier.weight(1f), enabled = !busy, height = 52.dp) {
                            SleepTimerService.test(ctx, k, TEST_DELAY_SEC)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            val fullSec = (if (prefs.fadeEnabled) prefs.fadeSeconds else 0) + 20
            PrimaryButton("전체 흐름 테스트 (${fullSec}초 타이머)", enabled = !busy) {
                SleepTimerService.startMillis(ctx, fullSec * 1000L, "검증")
            }
            if (busy) {
                Text("진행 중: ${ui.testLabel ?: ui.phase.name}", fontSize = 13.sp, color = C.accentText)
            }
        }

        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("기록", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.weight(1f))
                PillButton("공유") {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, VerifyLog.export())
                    ctx.startActivity(Intent.createChooser(send, "검증 기록 공유"))
                }
                Spacer(Modifier.width(8.dp))
                PillButton("지우기") { VerifyLog.clear() }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(12.dp)).background(C.bg).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(lines) { line ->
                    Text(line, fontSize = 11.sp, lineHeight = 15.sp, fontFamily = FontFamily.Monospace, color = C.text)
                }
            }
        }
    }
}
