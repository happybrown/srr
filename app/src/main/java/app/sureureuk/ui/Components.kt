package app.sureureuk.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.R

@Composable
fun MoonIcon(size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_moon), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    // 화면 본문은 좌우 20dp 여백 안에 있다. 뒤로 화살표가 그 선에 맞도록 버튼을 왼쪽으로 당긴다.
    Row(Modifier.fillMaxWidth().height(56.dp).offset(x = (-14).dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = "뒤로", tint = C.text, modifier = Modifier.size(22.dp))
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.text, modifier = Modifier.semantics { heading() })
    }
}

@Composable
fun StatusPill(text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF1F2140)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(C.accent))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.accentText)
    }
}

private val BUTTON_PADDING = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

@Composable
private fun ButtonContent(text: String, icon: Int?, fontSize: Int) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(
        text.keepAll(), fontSize = fontSize.sp, textAlign = TextAlign.Center,
        fontWeight = if (fontSize >= 17) FontWeight.Bold else FontWeight.SemiBold,
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Int? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = BUTTON_PADDING,
        colors = ButtonDefaults.buttonColors(
            containerColor = C.accent, contentColor = C.onAccent,
            disabledContainerColor = C.surface2, disabledContentColor = C.muted,
        ),
    ) {
        ButtonContent(text, icon, 18)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    icon: Int? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(18.dp),
        contentPadding = BUTTON_PADDING,
        colors = ButtonDefaults.buttonColors(
            containerColor = C.surface2, contentColor = C.text,
            disabledContainerColor = C.surface, disabledContentColor = C.muted,
        ),
    ) {
        ButtonContent(text, icon, 15)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, icon: Int? = null, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = BUTTON_PADDING,
        border = BorderStroke(1.dp, C.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = C.text),
    ) {
        ButtonContent(text, icon, 15)
    }
}

@Composable
fun SmallOutlineButton(text: String, description: String? = null, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).semantics { if (description != null) contentDescription = description },
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        border = BorderStroke(1.dp, C.accent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = C.accentText),
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.bg).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (on) C.accentSoft else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) C.text else C.muted, maxLines = 1)
            }
        }
    }
}

@Composable
fun WarningBanner(text: String, action: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    NoticeBanner(text, action, C.warnSoft, C.warn, C.warn, modifier, onClick)
}

/** 꼭 필요하진 않은 권한 안내: 경고보다 차분한 색 */
@Composable
fun TipBanner(text: String, action: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    NoticeBanner(text, action, C.surface2, C.muted, C.accentText, modifier, onClick)
}

@Composable
private fun NoticeBanner(text: String, action: String, bg: Color, fg: Color, actionColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(bg)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.keepAll(), fontSize = 14.sp, color = fg, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(action, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = actionColor, maxLines = 1, softWrap = false)
    }
}
