package app.sureureuk.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.sureureuk.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** 다이얼이 화면 어디에 있는지 (별이 다이얼·숫자 위에 뜨지 않게) */
val LocalDialBounds = compositionLocalOf<MutableState<Rect?>?> { null }

/** 다이얼 위치를 LocalDialBounds에 알린다 */
fun Modifier.reportDialBounds(state: MutableState<Rect?>?): Modifier =
    if (state == null) this else onGloballyPositioned { state.value = it.boundsInRoot() }

/**
 * 밤하늘: 은은하게 깜빡이는 별.
 * 다이얼 원 안쪽과 다이얼 아래(버튼·글자 영역)에는 그리지 않아 먼지처럼 보이지 않게 한다.
 */
@Composable
fun StarField(modifier: Modifier = Modifier, dialInRoot: Rect?, count: Int = 56, seed: Int = 7) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val stars = remember(count, seed) {
        val r = Random(seed)
        List(count) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.5f + r.nextFloat() * 1.2f, r.nextFloat()) }
    }
    val t by rememberInfiniteTransition(label = "stars").animateFloat(
        0f, 1f, infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "twinkle",
    )
    Canvas(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        val dial = dialInRoot?.translate(-origin) ?: return@Canvas
        val keepOut = dial.width / 2f + 6.dp.toPx()
        stars.forEach { (x, y, radius, phase) ->
            val p = Offset(x * size.width, y * size.height)
            if (p.y > dial.bottom) return@forEach
            if ((p - dial.center).getDistance() < keepOut) return@forEach
            val a = 0.15f + 0.35f * (0.5f + 0.5f * sin(((t + phase) * 2 * PI).toFloat()))
            drawCircle(Color.White.copy(alpha = a), radius.dp.toPx(), p)
        }
    }
}

/** 재생 중 표시: 오르내리는 막대 세 개 */
@Composable
fun EqualizerBars(color: Color, modifier: Modifier = Modifier.size(16.dp)) {
    val p by rememberInfiniteTransition(label = "eq").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1_100, easing = LinearEasing), RepeatMode.Restart), label = "eqp",
    )
    Canvas(modifier) {
        val w = size.width / 5f
        for (i in 0..2) {
            val h = size.height * (0.3f + 0.7f * abs(sin(((p + i * 0.31f) * PI).toFloat())))
            drawRoundRect(color, Offset(i * 2 * w, size.height - h), Size(w, h), CornerRadius(w / 2, w / 2))
        }
    }
}

/** 둥근 사각 안의 아이콘. done이면 오른쪽 아래에 체크를 단다. */
@Composable
fun IconBadge(icon: Int, tint: Color = C.accent, bg: Color = C.surface2, size: Dp = 40.dp, done: Boolean = false) {
    Box(Modifier.size(size)) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
        if (done) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.45f).clip(CircleShape).background(C.mint)
                    .border(2.dp, C.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = "허용됨", tint = C.bg, modifier = Modifier.size(size * 0.25f))
            }
        }
    }
}

/**
 * 끝날 때 할 일을 아이콘 세 개와 이어진 선으로: 🔉 ─ ⏸ ─ 📱 (아래에 짧은 이름)
 * 꺼 둔 단계는 흐리게.
 */
@Composable
fun StepsTimeline(steps: List<Triple<Int, Boolean, String>>, modifier: Modifier = Modifier, iconSize: Dp = 40.dp) {
    androidx.compose.foundation.layout.Row(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "타이머가 끝나면 " + steps.filter { it.second }.joinToString(", ") { it.third }.ifEmpty { "아무것도 하지 않아요" }
        },
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { i, (icon, on, label) ->
            if (i > 0) {
                Canvas(Modifier.weight(1f).padding(top = iconSize / 2 - 1.dp).height(2.dp)) {
                    drawLine(
                        if (on && steps[i - 1].second) C.accent.copy(alpha = 0.6f) else C.line,
                        Offset(2.dp.toPx(), size.height / 2), Offset(size.width - 2.dp.toPx(), size.height / 2),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            }
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(if (on) 1f else 0.4f),
            ) {
                Box(
                    Modifier.size(iconSize).clip(CircleShape).background(if (on) C.accentSoft else C.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(icon), contentDescription = null, tint = if (on) C.accentText else C.muted, modifier = Modifier.size(iconSize * 0.5f))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                androidx.compose.material3.Text(label, fontSize = 12.sp, color = if (on) C.text else C.muted, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** 볼륨이 줄어드는 모양: 끝나기 전 fadeSeconds 동안 서서히 0으로 (최대 5분 기준 비율) */
@Composable
fun FadeCurve(fadeSeconds: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = h * 0.2f
        val frac = (fadeSeconds / 300f).coerceIn(0.08f, 1f) * 0.75f
        val startX = w * (1f - frac)
        val area = Path().apply {
            moveTo(0f, h)
            lineTo(0f, top)
            lineTo(startX, top)
            // 귀에 고르게 들리도록 처음엔 천천히, 끝으로 갈수록 빠르게 (지수 모양 근사)
            cubicTo(startX + (w - startX) * 0.5f, top, w - (w - startX) * 0.2f, h * 0.75f, w, h)
            close()
        }
        drawPath(area, C.accent.copy(alpha = 0.18f))
        val line = Path().apply {
            moveTo(0f, top)
            lineTo(startX, top)
            cubicTo(startX + (w - startX) * 0.5f, top, w - (w - startX) * 0.2f, h * 0.75f, w, h)
        }
        drawPath(line, C.accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawLine(C.line, Offset(0f, h), Offset(w, h), strokeWidth = 1.dp.toPx())
        drawCircle(C.accent, 4.dp.toPx(), Offset(startX, top))
    }
}

/**
 * 누르면 한 번, 누르고 있으면 반복(점점 빠르게, 나중엔 큰 단계로).
 * TalkBack에서는 일반 버튼처럼 두 번 탭으로 한 번 실행된다.
 */
@Composable
fun HoldRepeatButton(
    icon: Int,
    description: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onStep: (big: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val step by rememberUpdatedState(onStep)
    val canStep by rememberUpdatedState(enabled)
    Box(
        modifier.size(52.dp).clip(CircleShape).background(C.surface).border(1.dp, C.line, CircleShape)
            .alpha(if (enabled) 1f else 0.35f)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { if (canStep) step(false); canStep }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!canStep) return@awaitEachGesture
                    down.consume()
                    step(false)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val job = scope.launch {
                        delay(450)
                        var n = 0
                        while (isActive && canStep) {
                            step(n >= 12) // 1초쯤 지나면 5분 단위로
                            if (n % 5 == 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            n++
                            delay(if (n < 12) 110 else 160)
                        }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = C.text, modifier = Modifier.size(22.dp))
    }
}

/** 설치된 앱의 아이콘 (보이지 않는 앱이면 null) */
@Composable
fun rememberAppIcon(pkg: String?): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(pkg) {
        if (pkg == null) null
        else try {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun AppIcon(bitmap: ImageBitmap, size: Dp = 32.dp) {
    Image(bitmap, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.25f)))
}

/** 앱 이름: 자주 쓰는 앱은 한국어 이름, 그 밖에는 시스템에 등록된 이름 */
@Composable
fun rememberAppLabel(pkg: String?): String {
    val ctx = LocalContext.current
    return remember(pkg) {
        if (pkg == null) return@remember ""
        val known = app.sureureuk.core.Fmt.appName(pkg)
        if (known != pkg) return@remember known
        try {
            ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }
}
