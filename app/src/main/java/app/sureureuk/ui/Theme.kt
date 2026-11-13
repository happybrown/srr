package app.sureureuk.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
        content = content,
    )
}
