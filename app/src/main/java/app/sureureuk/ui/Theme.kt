package app.sureureuk.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** 디자인 시안의 색 (밤에 쓰는 앱이라 어두운 화면만 제공) */
object C {
    val bg = Color(0xFF0E1020)
    val surface = Color(0xFF181B30)
    val surface2 = Color(0xFF22263F)
    val line = Color(0xFF2A2F4D)
    val text = Color(0xFFEEF0FF)
    val muted = Color(0xFF9CA3C7)
    val accent = Color(0xFFA99BFF)
    val accentSoft = Color(0xFF2A2650)
    val accentText = Color(0xFFC9C0FF)
    val onAccent = Color(0xFF14122B)
    val amber = Color(0xFFFFD58A)
    val mint = Color(0xFF8EE3C8)
    val mintSoft = Color(0xFF17302B)
    val warn = Color(0xFFFFC199)
    val warnSoft = Color(0xFF3A2A1E)
    val iconBg = Color(0xFF1F2140)
}

/**
 * 한국어 줄바꿈: 안드로이드 기본값은 한글을 글자 단위로 끊어 "버튼/을", "있어/요"처럼 된다.
 * Android 13+에서는 어절(띄어쓰기) 단위로 끊도록 지정한다. 그보다 낮은 버전에서는 무시된다.
 */
private val KoreanLineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase,
)

/**
 * 한글용 글자 모양: Material 기본 자간(bodyLarge 0.5sp 등)은 한글을 헐겁게 만들고,
 * keepAll()이 넣은 연결 문자마다 자간이 한 번 더 붙는다. 자간은 0, 줄 높이는 글자 크기의 1.45배로.
 */
private fun TextStyle.korean() = copy(
    lineBreak = KoreanLineBreak,
    letterSpacing = 0.sp,
    lineHeight = 1.45.em,
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.korean(), displayMedium = t.displayMedium.korean(), displaySmall = t.displaySmall.korean(),
        headlineLarge = t.headlineLarge.korean(), headlineMedium = t.headlineMedium.korean(), headlineSmall = t.headlineSmall.korean(),
        titleLarge = t.titleLarge.korean(), titleMedium = t.titleMedium.korean(), titleSmall = t.titleSmall.korean(),
        bodyLarge = t.bodyLarge.korean(), bodyMedium = t.bodyMedium.korean(), bodySmall = t.bodySmall.korean(),
        labelLarge = t.labelLarge.korean(), labelMedium = t.labelMedium.korean(), labelSmall = t.labelSmall.korean(),
    )
}

@Composable
fun SureureukTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.accent,
            onPrimary = C.onAccent,
            background = C.bg,
            onBackground = C.text,
            surface = C.surface,
            onSurface = C.text,
            surfaceVariant = C.surface2,
            onSurfaceVariant = C.muted,
            outline = C.line,
        ),
        typography = AppTypography,
        content = content,
    )
}
