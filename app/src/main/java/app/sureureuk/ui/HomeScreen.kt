package app.sureureuk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sureureuk.R
import app.sureureuk.core.Fmt
import app.sureureuk.core.PermState
import app.sureureuk.core.Phase
import app.sureureuk.core.Prefs
import app.sureureuk.core.TimerState
import app.sureureuk.core.TimerUi
import app.sureureuk.media.MediaStopper
import app.sureureuk.service.SleepTimerService
import app.sureureuk.update.Updates
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val PRESETS = listOf(15, 30, 45, 60, 90)
private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 120
private const val STEP = 5
private val GAP = 12.dp
private val MIN_DIAL = 150.dp

@Composable
fun HomeScreen(
    prefs: Prefs,
    perms: PermState,
    onStart: (Int) -> Unit,
    onSettings: () -> Unit,
    onPerms: () -> Unit,
) {
    val ctx = LocalContext.current
    val ui by TimerState.flow.collectAsState()
    // 실수로 타이머를 껐을 때 되돌리기: 끈 순간의 남은 시간
    var undoMs by remember { mutableStateOf<Long?>(null) }

    if (ui.active) {
        RunningScreen(ui, prefs, perms, onSettings, onPerms) { left ->
            undoMs = left
            SleepTimerService.cancel(ctx, "앱")
        }
    } else {
        SetupScreen(
            prefs, perms, onStart, onSettings, onPerms,
            undoMs = undoMs,
            onUndo = { ms ->
                undoMs = null
                SleepTimerService.startMillis(ctx, ms, "되돌리기")
            },
            onUndoExpired = { undoMs = null },
        )
    }
}

/**
 * 상단 바 + 스크롤되는 본문 + 하단 고정 버튼.
 * 다이얼 크기 = min(화면 폭 기준, 본문 높이 − 다른 요소 높이(reserved)) 이라서
 * 보통 폰에서는 스크롤 없이 한눈에 들어오고, 작은 화면·큰 글꼴에서는 겹치지 않고 본문만 스크롤된다.
 * 아래로 더 내용이 있으면 하단에 옅은 그림자로 알려 준다.
 */
@Composable
private fun HomeScaffold(
    running: Boolean,
    onSettings: () -> Unit,
    reserved: Dp,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.(dial: Dp) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        HomeTopBar(running, onSettings)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxDial = ((maxWidth - 40.dp) * 0.86f).coerceIn(MIN_DIAL, 320.dp)
            val dial = (maxHeight - reserved).coerceIn(MIN_DIAL, maxDial)
            val viewport = maxHeight
            val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(scroll)
                    .heightIn(min = viewport)
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(GAP),
            ) {
                content(dial)
            }
            if (scroll.canScrollForward) {
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, C.bg))),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().background(C.bg).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = bottom,
        )
    }
}

@Composable
private fun HomeTopBar(running: Boolean, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoonIcon(22.dp, C.amber)
        Spacer(Modifier.width(8.dp))
        Text("스르륵", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.text)
        Spacer(Modifier.weight(1f))
        if (running) {
            StatusPill("실행 중")
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = onSettings) {
            Icon(painterResource(R.drawable.ic_tune), contentDescription = "설정", tint = C.text, modifier = Modifier.size(22.dp))
        }
    }
}

// ---------------------------------------------------------------- 타이머 설정

@Composable
private fun SetupScreen(
    prefs: Prefs,
    perms: PermState,
    onStart: (Int) -> Unit,
    onSettings: () -> Unit,
    onPerms: () -> Unit,
    undoMs: Long?,
    onUndo: (Long) -> Unit,
    onUndoExpired: () -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(prefs.lastMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES)) }
    val set: (Int) -> Unit = { minutes = it.coerceIn(MIN_MINUTES, MAX_MINUTES) }
    val update by Updates.latest.collectAsState()

    LaunchedEffect(undoMs) {
        if (undoMs != null) {
            delay(5_000)
            onUndoExpired()
        }
    }

    // 다이얼을 뺀 본문 높이 (글꼴 크기에 따라 늘어나는 부분은 fontScale 반영)
    val fs = LocalDensity.current.fontScale
    val reserved = 24.dp + GAP * 4 +
        32.dp * fs + // 제목
        20.dp + 52.dp + // −/+ 줄
        20.dp + 48.dp + // 프리셋
        88.dp * fs + // 끝나면 요약
        (if (!perms.requiredOk) 48.dp * fs + GAP else 0.dp) +
        (if (update != null) 48.dp * fs + GAP else 0.dp)

    HomeScaffold(
        running = false,
        onSettings = onSettings,
        reserved = reserved,
        bottom = {
            if (undoMs != null) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(C.surface2)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("타이머를 껐어요", fontSize = 14.sp, color = C.text, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onUndo(undoMs) }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("되돌리기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.accentText)
                    }
                }
            }
            PrimaryButton("${minutes}분 타이머 시작") {
                prefs.lastMinutes = minutes
                onStart(minutes)
            }
            Text(
                "시작하면 보던 앱으로 돌아가요",
                fontSize = 13.sp, color = C.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { dial ->
        if (!perms.requiredOk) WarningBanner("필수 권한이 꺼져 있어요", "허용하기", onPerms)
        UpdateBanner()

        Text(
            "몇 분 뒤에 잠들까요?",
            fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = C.text,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SetupDial(dial, minutes, set)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepButton(R.drawable.ic_minus, "5분 줄이기", enabled = minutes > MIN_MINUTES) { set(minutes - STEP) }
                // 다이얼에 분이 크게 보이므로 여기서는 '언제 끝나는지'만
                Text(
                    "${Fmt.clock(System.currentTimeMillis() + minutes * 60_000L)}에 끝나요".keepAll(),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                StepButton(R.drawable.ic_plus, "5분 늘리기", enabled = minutes < MAX_MINUTES) { set(minutes + STEP) }
            }
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { v ->
                    val on = v == minutes
                    // 선택된 칩은 시작 버튼보다 어둡게: 밤에 밝은 덩어리가 둘이 되지 않도록
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (on) C.accentSoft else C.surface)
                            .border(if (on) 1.5.dp else 1.dp, if (on) C.accent else C.line, RoundedCornerShape(12.dp))
                            .selectable(selected = on, role = Role.RadioButton) { set(v) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${v}분", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                            color = if (on) C.accentText else C.text,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        EndActionsRow(prefs, onSettings)
    }
}

/** 다이얼 링의 반지름·두께(px). 그리기와 터치 판정이 같은 값을 쓴다. */
private fun Density.ringGeometry(minDimension: Float): Pair<Float, Float> {
    val outer = minDimension / 2f
    val stroke = (outer * 0.1f).coerceIn(10.dp.toPx(), 16.dp.toPx())
    return (outer - 16.dp.toPx() - stroke / 2f) to stroke
}

/**
 * 링 근처를 누르거나 끌 때만 5분 단위로 바뀐다. 가운데 숫자나 모서리를 누르면 아무 일도 없고,
 * 그곳에서 시작한 세로 스와이프는 화면 스크롤로 넘어간다. 12시를 넘겨도 120분↔5분으로 튀지 않는다.
 */
@Composable
private fun SetupDial(size: Dp, minutes: Int, set: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(minutes)
    val onSet by rememberUpdatedState(set)

    fun update(value: Int) {
        val v = value.coerceIn(MIN_MINUTES, MAX_MINUTES)
        if (v != current) {
            onSet(v)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val gestures = Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val w = this.size.width.toFloat()
                val h = this.size.height.toFloat()
                val (ring, _) = ringGeometry(minOf(w, h))
                val dist = (down.position - Offset(w / 2f, h / 2f)).getDistance()
                if (abs(dist - ring) > 28.dp.toPx()) return@awaitEachGesture // 링 밖: 소비하지 않음
                down.consume()
                update(angleToMinutes(down.position, w, h))
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.positionChanged()) {
                        change.consume()
                        var m = angleToMinutes(change.position, w, h)
                        val prev = current
                        if (prev >= 90 && m <= 30) m = MAX_MINUTES // 시계 방향으로 12시를 넘어감
                        else if (prev <= 30 && m >= 90) m = MIN_MINUTES // 반시계 방향으로 12시를 넘어감
                        update(m)
                    }
                }
            }
        }
        .semantics {
            contentDescription = "타이머 시간"
            stateDescription = Fmt.minutesLabel(minutes)
            progressBarRangeInfo = ProgressBarRangeInfo(
                minutes.toFloat(), MIN_MINUTES.toFloat()..MAX_MINUTES.toFloat(), steps = (MAX_MINUTES - MIN_MINUTES) / STEP - 1,
            )
            setProgress { target ->
                update(((target / STEP).roundToInt() * STEP))
                true
            }
        }

    // 숫자는 다이얼 크기에 맞춰 그린다(원 밖으로 넘치지 않게). 단위 '분'은 글꼴 크기 설정을 따른다.
    val density = LocalDensity.current
    // 세 자리(100~120)는 조금 작게, 단위는 글꼴 설정을 따르되 작은 다이얼에서 링을 넘지 않게
    val numberSize = with(density) { (size * if (minutes >= 100) 0.25f else 0.30f).toSp() }
    val unitSize = with(density) { minOf(16.sp.toDp(), size * 0.09f).toSp() }
    Dial(size = size, progress = minutes / MAX_MINUTES.toFloat(), knob = true, modifier = gestures) {
        NumberWithUnit("$minutes", "분", numberSize, unitSize, Modifier.align(Alignment.Center))
    }
}

/** 숫자는 정확히 가운데, 단위는 숫자 오른쪽 아래에 매단다 (단위 때문에 숫자가 왼쪽으로 밀리지 않게). */
@Composable
private fun NumberWithUnit(number: String, unit: String, numberSize: TextUnit, unitSize: TextUnit, modifier: Modifier = Modifier) {
    val tnum = LocalTextStyle.current.copy(fontFeatureSettings = "tnum", lineHeight = 1.1.em)
    Layout(
        modifier = modifier,
        content = {
            Text(number, style = tnum, fontSize = numberSize, fontWeight = FontWeight.Medium, color = C.text, maxLines = 1, softWrap = false)
            Text(unit, fontSize = unitSize, fontWeight = FontWeight.SemiBold, color = C.muted, maxLines = 1, softWrap = false)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val n = measurables[0].measure(loose)
        val u = measurables[1].measure(loose)
        val gap = 4.dp.roundToPx()
        val width = n.width + 2 * (gap + u.width)
        layout(width, n.height) {
            val nx = (width - n.width) / 2
            n.place(nx, 0)
            u.place(nx + n.width + gap, n[FirstBaseline] - u[FirstBaseline])
        }
    }
}

/** 0(12시) ~ 120분, 5분 단위 */
private fun angleToMinutes(p: Offset, w: Float, h: Float): Int {
    val dx = p.x - w / 2f
    val dy = p.y - h / 2f
    var deg = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble()))
    if (deg < 0) deg += 360.0
    return (deg / 360.0 * MAX_MINUTES / STEP).roundToInt() * STEP
}

@Composable
private fun StepButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(C.surface).border(1.dp, C.line, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = C.text, modifier = Modifier.size(22.dp))
    }
}

/** 켜 둔 동작을 한 문장으로: 띄어쓰기에서만 줄이 바뀌고, 화살표 기호를 읽어 주지 않는다. */
private fun endSummary(prefs: Prefs): String {
    val d = Fmt.duration(prefs.fadeSeconds)
    val f = prefs.fadeEnabled
    val s = prefs.stopEnabled
    val c = prefs.screenEnabled
    return when {
        f && s && c -> "소리를 ${d}간 줄이고, 재생을 멈춘 뒤 화면을 꺼요"
        f && s -> "소리를 ${d}간 줄이고 재생을 멈춰요"
        f && c -> "소리를 ${d}간 줄이고 화면을 꺼요"
        s && c -> "재생을 멈추고 화면을 꺼요"
        f -> "소리를 ${d}간 줄여요"
        s -> "재생을 멈춰요"
        c -> "화면을 꺼요"
        else -> "아무것도 하지 않아요"
    }
}

@Composable
private fun EndActionsRow(prefs: Prefs, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp)).background(C.surface)
            .clickable(role = Role.Button, onClickLabel = "끝날 때 동작 바꾸기", onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("타이머가 끝나면", fontSize = 13.sp, color = C.muted)
            Text(endSummary(prefs).keepAll(), fontSize = 15.sp, color = C.text)
        }
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = C.muted, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------- 실행 중

@Composable
private fun RunningScreen(
    ui: TimerUi,
    prefs: Prefs,
    perms: PermState,
    onSettings: () -> Unit,
    onPerms: () -> Unit,
    onCancel: (leftMs: Long) -> Unit,
) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val stopper = remember { MediaStopper(ctx) }
    var playing by remember { mutableStateOf("확인 중이에요") }
    LaunchedEffect(ui.phase, perms.listener) {
        while (true) {
            playing = stopper.nowPlayingLabel()
            delay(3_000)
        }
    }
    val left = (ui.endAt - now).coerceAtLeast(0)
    val total = (ui.totalMinutes * 60_000L).coerceAtLeast(1)

    val fs = LocalDensity.current.fontScale
    val reserved = 24.dp + GAP * 3 + 16.dp + 52.dp * fs + 64.dp * fs // 여백 + 다이얼 아래 글 + 재생 중 줄

    HomeScaffold(
        running = true,
        onSettings = onSettings,
        reserved = reserved,
        bottom = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("+5분", Modifier.weight(1f)) { SleepTimerService.extend(ctx, 5, "앱") }
                SecondaryButton("+15분", Modifier.weight(1f)) { SleepTimerService.extend(ctx, 15, "앱") }
            }
            Spacer(Modifier.height(8.dp)) // 졸린 손가락이 '끄기'를 잘못 누르지 않게 간격을 더 둔다
            GhostButton("타이머 끄기", Modifier.fillMaxWidth()) { onCancel(left) }
        },
    ) { dial ->
        Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val s = (left + 999) / 1000
            val density = LocalDensity.current
            // 링 안쪽에 들어가도록 다이얼 크기에 맞춘다 (1시간 이상은 글자가 많아 조금 작게)
            val numberSize = with(density) { (dial * if (s >= 3600) 0.17f else 0.21f).toSp() }
            // '남은 시간'은 글꼴 설정을 따르되 작은 다이얼에서는 링을 넘지 않게
            val labelSize = with(density) { minOf(13.sp.toDp(), dial * 0.08f).toSp() }
            val clockText = if (s >= 3600) {
                // 1시간 이상: 시:분을 크게, 초는 작게
                buildAnnotatedString {
                    append("%d:%02d".format(s / 3600, (s % 3600) / 60))
                    withStyle(SpanStyle(fontSize = numberSize * 0.5f)) { append(":%02d".format(s % 60)) }
                }
            } else {
                buildAnnotatedString { append("%d:%02d".format(s / 60, s % 60)) }
            }
            val spoken = buildString {
                append("남은 시간 ")
                if (s >= 3600) append("${s / 3600}시간 ")
                append("${(s % 3600) / 60}분 ${s % 60}초")
            }
            Dial(
                size = dial, progress = left.toFloat() / total, knob = false, ticks = false,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            ) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("남은 시간", fontSize = labelSize, color = C.muted, maxLines = 1)
                    Text(
                        clockText,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum", lineHeight = 1.1.em),
                        fontSize = numberSize, fontWeight = FontWeight.Medium, color = C.text, maxLines = 1, softWrap = false,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${Fmt.clock(ui.endAt)}에 끝나요".keepAll(),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text, textAlign = TextAlign.Center,
                )
                Text(
                    when (ui.phase) {
                        Phase.FADING -> "소리를 서서히 줄이고 있어요"
                        Phase.ENDING -> "재생을 멈추고 화면을 끄는 중이에요"
                        else -> if (prefs.fadeEnabled) "끝나기 ${Fmt.duration(prefs.fadeSeconds)} 전부터 소리를 줄여요" else "끝나면 바로 재생을 멈춰요"
                    }.keepAll(),
                    fontSize = 13.sp, color = C.muted, textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (!perms.listener) {
            // 이게 없으면 넷플릭스가 멈추지 않을 수 있다: 바로 고칠 수 있게
            WarningBanner("알림 접근을 허용해야 영상을 확실히 멈춰요", "허용하기", onPerms)
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_music), contentDescription = null, tint = C.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("지금 재생 중", fontSize = 13.sp, color = C.muted)
                Spacer(Modifier.width(8.dp))
                Text(playing.keepAll(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------- 다이얼

/** 원형 다이얼: 눈금(5분마다, 30분마다 굵게) + 진행 호 + 손잡이 */
@Composable
fun Dial(
    size: Dp,
    progress: Float,
    knob: Boolean,
    modifier: Modifier = Modifier,
    ticks: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = this.size.minDimension / 2f
            if (ticks) {
                val count = MAX_MINUTES / STEP
                for (i in 0 until count) {
                    val major = i % (30 / STEP) == 0
                    rotate(i * 360f / count) {
                        drawLine(
                            if (major) C.muted else C.line,
                            Offset(center.x, center.y - outer + 1.dp.toPx()),
                            Offset(center.x, center.y - outer + (if (major) 9.dp else 6.dp).toPx()),
                            strokeWidth = (if (major) 2.dp else 1.5.dp).toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            val (r, stroke) = ringGeometry(this.size.minDimension)
            drawCircle(C.surface2, radius = r, style = Stroke(stroke))
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    C.accent, -90f, sweep, false,
                    topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            if (knob) {
                val a = Math.toRadians((sweep - 90f).toDouble())
                val k = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
                val kr = stroke * 0.95f
                drawCircle(C.text, kr, k)
                drawCircle(C.accent, kr, k, style = Stroke(stroke * 0.3f))
            }
        }
        content()
    }
}
