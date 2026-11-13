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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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

/** 프리셋: 분 → 칩 글자 */
private val PRESETS = listOf(15 to "15분", 30 to "30분", 60 to "1시간", 120 to "2시간", 240 to "4시간")
const val MIN_MINUTES = 1
const val MAX_MINUTES = 8 * 60
private const val MAX_HOURS = MAX_MINUTES / 60
private val GAP = 12.dp
private val MIN_DIAL = 160.dp

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
 * 상단 바 + 별이 깔린 스크롤 본문 + 하단 고정 버튼.
 * 다이얼 크기 = min(화면 폭 기준, 본문 높이 − 다른 요소 높이(reserved)) 이라서
 * 보통 폰에서는 스크롤 없이 한눈에 들어오고, 작은 화면·큰 글꼴에서는 겹치지 않고 본문만 스크롤된다.
 */
@Composable
private fun HomeScaffold(
    status: String?,
    onSettings: () -> Unit,
    reserved: Dp,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.(dial: Dp) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        HomeTopBar(status, onSettings)
        val dialBounds = remember { mutableStateOf<Rect?>(null) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            StarField(Modifier.fillMaxSize(), dialBounds.value)
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
                CompositionLocalProvider(LocalDialBounds provides dialBounds) { content(dial) }
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
private fun HomeTopBar(status: String?, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoonIcon(22.dp, C.amber)
        Spacer(Modifier.width(8.dp))
        Text("스르륵", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.text)
        Spacer(Modifier.weight(1f))
        if (status != null) {
            StatusPill(status)
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
    val reserved = 24.dp + GAP * 3 +
        20.dp + 52.dp + // −/+ 줄
        20.dp + 48.dp + // 프리셋
        96.dp * fs + // 끝나면 단계
        (if (!perms.requiredOk) 48.dp * fs + GAP else 0.dp) +
        (if (update != null) 48.dp * fs + GAP else 0.dp)

    HomeScaffold(
        status = null,
        onSettings = onSettings,
        reserved = reserved,
        bottom = {
            if (undoMs != null) UndoBar { onUndo(undoMs) }
            PrimaryButton("${Fmt.minutesLabel(minutes)} 타이머 시작", icon = R.drawable.ic_play) {
                prefs.lastMinutes = minutes
                onStart(minutes)
            }
            Text(
                "시작하면 보던 앱으로 돌아가요",
                fontSize = 13.sp, color = C.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { dial ->
        if (!perms.requiredOk) WarningBanner("필수 권한이 꺼져 있어요", "허용하기", onClick = onPerms)
        UpdateBanner()

        Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SetupDial(dial, minutes, set)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 누르면 1분, 누르고 있으면 빠르게 반복하다 5분 단위로
                HoldRepeatButton(R.drawable.ic_minus, "1분 줄이기", enabled = minutes > MIN_MINUTES) { big ->
                    minutes = (minutes - if (big) 5 else 1).coerceIn(MIN_MINUTES, MAX_MINUTES)
                }
                Row(
                    Modifier.weight(1f).padding(horizontal = 8.dp).semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_moon), contentDescription = "끝나는 시각", tint = C.amber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Fmt.clock(System.currentTimeMillis() + minutes * 60_000L),
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.text, maxLines = 1, softWrap = false,
                    )
                }
                HoldRepeatButton(R.drawable.ic_plus, "1분 늘리기", enabled = minutes < MAX_MINUTES) { big ->
                    minutes = (minutes + if (big) 5 else 1).coerceIn(MIN_MINUTES, MAX_MINUTES)
                }
            }
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { (v, label) ->
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
                            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                            color = if (on) C.accentText else C.text,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        EndActionsCard(prefs, onSettings)
    }
}

@Composable
private fun UndoBar(onUndo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(C.surface2)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_stop), contentDescription = null, tint = C.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text("타이머를 껐어요", fontSize = 14.sp, color = C.text, modifier = Modifier.weight(1f))
        Box(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("되돌리기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.accentText)
        }
    }
}

/** 다이얼 링의 반지름·두께(px). 그리기와 터치 판정이 같은 값을 쓴다. */
private fun Density.ringGeometry(minDimension: Float): Pair<Float, Float> {
    val outer = minDimension / 2f
    val stroke = (outer * 0.1f).coerceIn(10.dp.toPx(), 16.dp.toPx())
    return (outer - 16.dp.toPx() - stroke / 2f) to stroke
}

/** 12시 = 0°, 시계 방향 0~360° */
private fun angleDeg(p: Offset, c: Offset): Float {
    var deg = Math.toDegrees(atan2((p.x - c.x).toDouble(), (c.y - p.y).toDouble())).toFloat()
    if (deg < 0) deg += 360f
    return deg
}

/**
 * 여러 바퀴 도는 다이얼: 한 바퀴 = 60분, 1분 단위, 최대 8시간.
 * - 링을 따라 돌리면 바퀴 수만큼 시간이 쌓이고 (6° = 1분)
 * - 링을 탭하면 지금 시간대 안에서 그 위치의 분으로 맞춘다.
 * - 링에서 떨어진 곳(가운데 숫자, 모서리)은 반응하지 않고, 그곳에서 시작한 스와이프는 화면 스크롤로 넘어간다.
 */
@Composable
private fun SetupDial(size: Dp, minutes: Int, set: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(minutes)
    val onSet by rememberUpdatedState(set)

    fun update(value: Int) {
        val v = value.coerceIn(MIN_MINUTES, MAX_MINUTES)
        if (v != current) {
            // 5분 눈금을 지날 때만 진동
            if (v / 5 != current / 5) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSet(v)
        }
    }

    val gestures = Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val (ring, _) = ringGeometry(minOf(this.size.width, this.size.height).toFloat())
                if (abs((down.position - c).getDistance() - ring) > 32.dp.toPx()) return@awaitEachGesture
                down.consume()
                val startMinutes = current
                var last = angleDeg(down.position, c)
                var acc = 0f
                var moved = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.positionChanged()) {
                        change.consume()
                        val a = angleDeg(change.position, c)
                        var d = a - last
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        last = a
                        acc += d
                        if (abs(acc) > 4f) moved = true
                        if (moved) update(startMinutes + (acc / 6f).roundToInt())
                    }
                }
                if (!moved) {
                    val minuteOfHour = (angleDeg(down.position, c) / 6f).roundToInt() % 60
                    update((current / 60) * 60 + minuteOfHour)
                }
            }
        }
        .semantics {
            contentDescription = "타이머 시간"
            stateDescription = Fmt.minutesLabel(minutes)
            progressBarRangeInfo = ProgressBarRangeInfo(minutes.toFloat(), MIN_MINUTES.toFloat()..MAX_MINUTES.toFloat())
            setProgress { target ->
                update(target.roundToInt())
                true
            }
        }

    val inHour = minutes % 60
    val progress = if (inHour == 0 && minutes > 0) 1f else inHour / 60f
    Dial(size = size, progress = progress, knob = true, modifier = gestures, ticks = 60, majorEvery = 5) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            DurationText(minutes, size)
            Spacer(Modifier.height(size * 0.04f))
            HourDots(minutes, size)
        }
    }
}

/** 큰 숫자 + 작은 단위: "45분", "1시간 30분", "2시간" */
@Composable
private fun DurationText(minutes: Int, dial: Dp) {
    val density = LocalDensity.current
    val h = minutes / 60
    val m = minutes % 60
    val big = with(density) { (dial * if (h > 0) 0.2f else 0.28f).toSp() }
    val unit = with(density) { minOf(15.sp.toDp(), dial * 0.075f).toSp() }
    val num = LocalTextStyle.current.copy(fontFeatureSettings = "tnum", lineHeight = 1.1.em)
    Row(verticalAlignment = Alignment.Bottom) {
        if (h > 0) {
            Text("$h", style = num, fontSize = big, fontWeight = FontWeight.Medium, color = C.text, maxLines = 1, modifier = Modifier.alignByBaseline())
            Text(
                "시간", fontSize = unit, fontWeight = FontWeight.SemiBold, color = C.muted, maxLines = 1,
                modifier = Modifier.alignByBaseline().padding(start = 2.dp, end = if (m > 0) 8.dp else 0.dp),
            )
        }
        if (h == 0 || m > 0) {
            Text(
                if (h > 0) "%02d".format(m) else "$m", style = num, fontSize = big, fontWeight = FontWeight.Medium, color = C.text,
                maxLines = 1, modifier = Modifier.alignByBaseline(),
            )
            Text("분", fontSize = unit, fontWeight = FontWeight.SemiBold, color = C.muted, maxLines = 1, modifier = Modifier.alignByBaseline().padding(start = 2.dp))
        }
    }
}

/** 몇 시간인지: 8칸 중 채운 칸 = 시간, 지금 채우는 칸은 테두리만 */
@Composable
private fun HourDots(minutes: Int, dial: Dp) {
    val full = minutes / 60
    val w = (dial * 0.045f).coerceIn(7.dp, 11.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(w * 0.45f), modifier = Modifier.clearAndSetSemantics { }) {
        for (i in 0 until MAX_HOURS) {
            val filled = i < full
            val partial = i == full && minutes % 60 > 0
            Box(
                Modifier.size(width = w, height = w * 0.5f).clip(RoundedCornerShape(50))
                    .background(if (filled) C.accent else C.surface2)
                    .border(1.dp, if (partial) C.accent else Color.Transparent, RoundedCornerShape(50)),
            )
        }
    }
}

/** 끝날 때 할 일을 아이콘으로: 🔉 ─ ⏸ ─ 📱 (누르면 설정) */
@Composable
private fun EndActionsCard(prefs: Prefs, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.surface)
            .clickable(role = Role.Button, onClickLabel = "끝날 때 동작 바꾸기", onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepsTimeline(
            listOf(
                Triple(R.drawable.ic_volume, prefs.fadeEnabled, "${Fmt.duration(prefs.fadeSeconds)} 줄이기"),
                Triple(R.drawable.ic_pause, prefs.stopEnabled, "멈추기"),
                Triple(R.drawable.ic_phone_off, prefs.screenEnabled, "화면 끄기"),
            ),
            Modifier.weight(1f),
        )
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = C.muted, modifier = Modifier.padding(start = 4.dp).size(20.dp))
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
    var playing by remember { mutableStateOf(emptyList<String>()) }
    var musicActive by remember { mutableStateOf(false) }
    LaunchedEffect(ui.phase, perms.listener) {
        while (true) {
            playing = stopper.playingPackages()
            musicActive = stopper.isMusicActive()
            delay(3_000)
        }
    }
    val total = (ui.totalMinutes * 60_000L).coerceAtLeast(1)
    val left = (ui.endAt - now).coerceIn(0, total)
    val start = ui.endAt - total
    val fadeMs = if (prefs.fadeEnabled) prefs.fadeSeconds * 1000L else 0L
    val fading = ui.phase == Phase.FADING || ui.phase == Phase.ENDING

    val fs = LocalDensity.current.fontScale
    val reserved = 24.dp + GAP * 4 + 16.dp + 56.dp * fs + 56.dp * fs // 여백 + 타임라인 + 재생 중

    HomeScaffold(
        status = if (fading) "소리 줄이는 중" else "실행 중",
        onSettings = onSettings,
        reserved = reserved,
        bottom = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("5분", Modifier.weight(1f), icon = R.drawable.ic_plus) { SleepTimerService.extend(ctx, 5, "앱") }
                SecondaryButton("15분", Modifier.weight(1f), icon = R.drawable.ic_plus) { SleepTimerService.extend(ctx, 15, "앱") }
            }
            Spacer(Modifier.height(8.dp)) // 졸린 손가락이 '끄기'를 잘못 누르지 않게 간격을 더 둔다
            GhostButton("타이머 끄기", Modifier.fillMaxWidth(), icon = R.drawable.ic_stop) { onCancel(left) }
        },
    ) { dial ->
        Spacer(Modifier.weight(1f))

        val s = (left + 999) / 1000
        val density = LocalDensity.current
        val numberSize = with(density) { (dial * if (s >= 3600) 0.17f else 0.21f).toSp() }
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
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Dial(
                size = dial, progress = left.toFloat() / total, knob = false, ticks = 0,
                fadeFraction = (fadeMs.toFloat() / total).coerceIn(0f, 1f),
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            ) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    // 지금 단계를 글 대신 아이콘으로: 기다리는 중 = 달, 소리 줄이는 중 = 스피커
                    Icon(
                        painterResource(if (fading) R.drawable.ic_volume else R.drawable.ic_moon),
                        contentDescription = null, tint = C.amber, modifier = Modifier.size((dial * 0.09f).coerceIn(16.dp, 26.dp)),
                    )
                    Text(
                        clockText,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum", lineHeight = 1.1.em),
                        fontSize = numberSize, fontWeight = FontWeight.Medium, color = C.text, maxLines = 1, softWrap = false,
                    )
                }
            }
        }

        TimelineBar(start = start, fadeAt = ui.endAt - fadeMs, end = ui.endAt, now = now, showFade = fadeMs > 0)

        Spacer(Modifier.weight(1f))

        if (!perms.listener) {
            // 이게 없으면 넷플릭스가 멈추지 않을 수 있다: 바로 고칠 수 있게
            WarningBanner("알림 접근을 허용해야 영상을 확실히 멈춰요", "허용하기", onClick = onPerms)
        } else {
            NowPlaying(playing, musicActive)
        }
    }
}

/**
 * ▶ 시작 ─────●────── 🔉 ━━━ 🌙 끝
 * 지금 위치, 소리를 줄이기 시작하는 지점, 끝나는 시각을 한 줄 막대로.
 */
@Composable
private fun TimelineBar(start: Long, fadeAt: Long, end: Long, now: Long, showFade: Boolean) {
    val span = (end - start).coerceAtLeast(1).toFloat()
    val p = ((now - start) / span).coerceIn(0f, 1f)
    val f = ((fadeAt - start) / span).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val y = size.height / 2
            val sw = 6.dp.toPx()
            val l = sw / 2
            val r = size.width - sw / 2
            val x = l + (r - l) * p
            drawLine(C.surface2, Offset(l, y), Offset(r, y), sw, StrokeCap.Round)
            if (showFade) drawLine(C.amber.copy(alpha = 0.45f), Offset(l + (r - l) * f, y), Offset(r, y), sw, StrokeCap.Round)
            drawLine(C.accent, Offset(l, y), Offset(x, y), sw, StrokeCap.Round)
            if (showFade) drawCircle(C.amber, 4.dp.toPx(), Offset(l + (r - l) * f, y))
            drawCircle(C.text, 7.dp.toPx(), Offset(x, y))
            drawCircle(C.accent, 7.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = C.muted, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(Fmt.clock(start), fontSize = 12.sp, color = C.muted, maxLines = 1, softWrap = false)
            Spacer(Modifier.weight(1f))
            if (showFade) {
                Icon(painterResource(R.drawable.ic_volume), contentDescription = null, tint = C.amber, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(Fmt.clock(fadeAt), fontSize = 12.sp, color = C.amber, maxLines = 1, softWrap = false)
                Spacer(Modifier.weight(1f))
            }
            Text(Fmt.clock(end), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.text, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(4.dp))
            Icon(painterResource(R.drawable.ic_moon), contentDescription = null, tint = C.amber, modifier = Modifier.size(13.dp))
        }
    }
}

/** 지금 재생 중인 앱: 앱 아이콘 + 이름 + 움직이는 막대 */
@Composable
private fun NowPlaying(packages: List<String>, musicActive: Boolean) {
    val first = packages.firstOrNull()
    val icon = rememberAppIcon(first)
    val label = rememberAppLabel(first)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (first != null && icon != null) {
            AppIcon(icon, 32.dp)
        } else {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_music), contentDescription = null, tint = C.muted, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            when {
                first != null -> if (packages.size > 1) "$label 외 ${packages.size - 1}개" else label
                musicActive -> "다른 앱에서 재생 중이에요"
                else -> "재생 중인 앱이 없어요"
            }.keepAll(),
            fontSize = 15.sp, fontWeight = if (first != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (first != null) C.text else C.muted, modifier = Modifier.weight(1f),
        )
        if (first != null || musicActive) EqualizerBars(C.accent, Modifier.size(18.dp))
    }
}

// ---------------------------------------------------------------- 다이얼

/**
 * 원형 다이얼: 눈금 + 진행 호 + 손잡이.
 * fadeFraction > 0이면 남은 호 중 '소리 줄이는 구간'(끝나기 직전)을 호박색으로 칠한다.
 */
@Composable
fun Dial(
    size: Dp,
    progress: Float,
    knob: Boolean,
    modifier: Modifier = Modifier,
    ticks: Int = 60,
    majorEvery: Int = 5,
    fadeFraction: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.size(size).reportDialBounds(LocalDialBounds.current)) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = this.size.minDimension / 2f
            for (i in 0 until ticks) {
                val major = i % majorEvery == 0
                rotate(i * 360f / ticks) {
                    drawLine(
                        if (major) C.muted else C.line,
                        Offset(center.x, center.y - outer + 1.dp.toPx()),
                        Offset(center.x, center.y - outer + (if (major) 8.dp else 4.dp).toPx()),
                        strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            val (r, stroke) = ringGeometry(this.size.minDimension)
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2, r * 2)
            drawCircle(C.surface2, radius = r, style = Stroke(stroke))
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(C.accent, -90f, sweep, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (fadeFraction > 0f) {
                    val fadeSweep = minOf(sweep, 360f * fadeFraction)
                    drawArc(C.amber, -90f, fadeSweep, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
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
