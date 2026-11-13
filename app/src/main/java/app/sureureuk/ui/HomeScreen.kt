package app.sureureuk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.core.Fmt
import app.sureureuk.core.Phase
import app.sureureuk.core.PermState
import app.sureureuk.core.Prefs
import app.sureureuk.core.TimerState
import app.sureureuk.core.TimerUi
import app.sureureuk.media.MediaStopper
import app.sureureuk.service.SleepTimerService
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeScreen(
    prefs: Prefs,
    perms: PermState,
    onStart: (Int) -> Unit,
    onSettings: () -> Unit,
    onVerify: () -> Unit,
    onPerms: () -> Unit,
) {
    val ui by TimerState.flow.collectAsState()
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            MoonIcon(22.dp, C.amber)
            Spacer(Modifier.width(8.dp))
            Text("스르륵", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.text)
            Spacer(Modifier.weight(1f))
            if (ui.active) {
                StatusPill("타이머 실행 중")
            } else {
                PillButton("검증", onVerify)
                Spacer(Modifier.width(8.dp))
                PillButton("설정", onSettings)
            }
        }
        if (!perms.requiredOk) WarningBanner("필수 권한이 꺼져 있어 일부 기능이 동작하지 않아요", "권한 설정", onPerms)
        if (ui.active) RunningContent(ui, prefs) else SetupContent(prefs, onStart, onSettings)
    }
}

@Composable
private fun ColumnScope.SetupContent(prefs: Prefs, onStart: (Int) -> Unit, onSettings: () -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(prefs.lastMinutes) }
    val set: (Int) -> Unit = { minutes = it.coerceIn(5, 120) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("몇 분 뒤에 잠들까요?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = C.text)
        Text("시간이 되면 소리를 줄이고, 재생을 멈추고, 화면을 꺼 드려요.", fontSize = 14.sp, color = C.muted)
    }

    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        val dialSize = minOf(264.dp, maxHeight - 72.dp).coerceAtLeast(150.dp)
        val scale = dialSize / 264.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Dial(size = dialSize, progress = minutes / 120f, knob = true, onDrag = set) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$minutes", fontSize = (76 * scale).sp, fontWeight = FontWeight.Medium, color = C.text)
                    Text("분", fontSize = (20 * scale).sp, fontWeight = FontWeight.SemiBold, color = C.muted, modifier = Modifier.padding(bottom = (14 * scale).dp))
                }
                Text("${Fmt.clock(System.currentTimeMillis() + minutes * 60_000L)}에 끝나요", fontSize = 14.sp, color = C.muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoundIconButton("−", "5분 줄이기") { set(minutes - 5) }
                Text("돌리거나 눌러서 조절", fontSize = 13.sp, color = C.muted, modifier = Modifier.width(130.dp), textAlign = TextAlign.Center)
                RoundIconButton("+", "5분 늘리기") { set(minutes + 5) }
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 30, 45, 60, 90).forEach { v ->
            val on = v == minutes
            Box(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (on) C.accent else C.surface)
                    .border(1.dp, if (on) C.accent else C.line, RoundedCornerShape(14.dp))
                    .selectable(selected = on, role = Role.RadioButton) { set(v) },
                contentAlignment = Alignment.Center,
            ) {
                Text("${v}분", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) C.onAccent else C.text)
            }
        }
    }

    EndSummaryCard(prefs, onSettings)

    PrimaryButton("${minutes}분 타이머 시작") {
        prefs.lastMinutes = minutes
        onStart(minutes)
    }
    Text("시작하면 보던 앱으로 바로 돌아가요", fontSize = 12.sp, color = C.muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun EndSummaryCard(prefs: Prefs, onClick: () -> Unit) {
    val steps = buildList {
        if (prefs.fadeEnabled) add("소리\n${Fmt.duration(prefs.fadeSeconds)}간 줄이기")
        if (prefs.stopEnabled) add("재생\n멈추기")
        if (prefs.screenEnabled) add("화면\n끄기")
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.surface)
            .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("타이머가 끝나면", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted, modifier = Modifier.weight(1f))
            Text("변경", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.accent)
        }
        if (steps.isEmpty()) {
            Text("아무 동작도 하지 않아요", fontSize = 14.sp, color = C.text)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                steps.forEachIndexed { i, s ->
                    if (i > 0) Text("›", fontSize = 18.sp, color = C.line)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(C.surface2), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.accent)
                        }
                        Text(s, fontSize = 13.sp, lineHeight = 17.sp, color = C.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.RunningContent(ui: TimerUi, prefs: Prefs) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val stopper = remember { MediaStopper(ctx) }
    var playing by remember { mutableStateOf("확인 중") }
    LaunchedEffect(ui.phase) {
        while (true) {
            playing = stopper.nowPlayingLabel()
            delay(3_000)
        }
    }
    val left = (ui.endAt - now).coerceAtLeast(0)
    val total = (ui.totalMinutes * 60_000L).coerceAtLeast(1)

    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        val dialSize = minOf(288.dp, maxHeight - 8.dp).coerceAtLeast(150.dp)
        val scale = dialSize / 288.dp
        Dial(size = dialSize, progress = left.toFloat() / total, knob = false, onDrag = null) {
            Text("남은 시간", fontSize = 14.sp, color = C.muted)
            Text(Fmt.mmss(left), fontSize = (64 * scale).sp, fontWeight = FontWeight.Medium, color = C.text)
            Text("${Fmt.clock(ui.endAt)}에 끝나요", fontSize = 14.sp, color = C.muted)
        }
    }

    Text(
        when (ui.phase) {
            Phase.FADING -> "소리를 서서히 줄이고 있어요"
            Phase.ENDING -> "재생을 멈추고 화면을 끄는 중이에요"
            else -> if (prefs.fadeEnabled) "끝나기 ${Fmt.duration(prefs.fadeSeconds)} 전부터 소리를 서서히 줄여요" else "끝나면 바로 재생을 멈춰요"
        },
        fontSize = 13.sp, color = C.muted,
    )

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("지금 재생 중인 앱", fontSize = 12.sp, color = C.muted)
            Text(playing, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("+5분", Modifier.weight(1f)) { SleepTimerService.extend(ctx, 5, "앱") }
        SecondaryButton("+15분", Modifier.weight(1f)) { SleepTimerService.extend(ctx, 15, "앱") }
    }
    GhostButton("타이머 끄기", Modifier.fillMaxWidth()) { SleepTimerService.cancel(ctx, "앱") }
}

/** 원형 다이얼. 드래그하면 5분 단위(5~120분)로 바뀐다. */
@Composable
fun Dial(
    size: Dp,
    progress: Float,
    knob: Boolean,
    onDrag: ((Int) -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dragCallback by rememberUpdatedState(onDrag)
    val gesture = if (onDrag != null) {
        Modifier.pointerInput(Unit) {
            fun toMinutes(p: Offset): Int {
                val dx = p.x - this.size.width / 2f
                val dy = p.y - this.size.height / 2f
                var deg = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble()))
                if (deg < 0) deg += 360.0
                return ((deg / 360.0 * 120 / 5).roundToInt() * 5).coerceIn(5, 120)
            }
            detectDragGestures(onDragStart = { dragCallback?.invoke(toMinutes(it)) }) { change, _ ->
                change.consume()
                dragCallback?.invoke(toMinutes(change.position))
            }
        }
    } else {
        Modifier
    }
    Box(Modifier.size(size).then(gesture), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = this.size.minDimension / 2f
            for (i in 0 until 60) {
                rotate(i * 6f) {
                    drawLine(
                        C.line,
                        Offset(center.x, center.y - outer + 2.dp.toPx()),
                        Offset(center.x, center.y - outer + 7.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
            }
            val stroke = 14.dp.toPx()
            val r = outer - 22.dp.toPx()
            drawCircle(C.surface2, radius = r, style = Stroke(stroke))
            val sweep = 360f * progress.coerceIn(0f, 1f)
            drawArc(
                C.accent, -90f, sweep, false,
                topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (knob) {
                val a = Math.toRadians((sweep - 90f).toDouble())
                val k = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
                drawCircle(C.text, 13.dp.toPx(), k)
                drawCircle(C.accent, 13.dp.toPx(), k, style = Stroke(4.dp.toPx()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}
