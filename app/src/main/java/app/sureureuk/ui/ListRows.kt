package app.sureureuk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.R

/*
 * 설정·기능 점검 화면의 공용 틀: 구역 이름 + 묶음 카드 + 아이콘이 붙은 행.
 * 행은 높이를 고정하지 않아 글자가 길거나 글꼴이 커져도 잘리지 않는다.
 */

/** 행의 글자가 시작하는 위치 (카드 여백 16 + 아이콘 40 + 간격 12) */
val ROW_INSET = 68.dp

/** 카드 안에서 행 아래에 붙는 안내·세부 설정의 여백 */
val ROW_EXTRA = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)

@Composable
fun SectionLabel(text: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing()
    }
}

@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.surface), content = content)
}

@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = ROW_INSET).height(1.dp).background(C.line.copy(alpha = 0.6f)))
}

@Composable
private fun RowScope.RowBody(icon: Int, tint: Color, title: String, desc: String?, trailing: @Composable () -> Unit) {
    IconBadge(icon, tint = tint)
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title.keepAll(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        if (desc != null) Text(desc.keepAll(), fontSize = 13.sp, color = C.muted)
    }
    Spacer(Modifier.width(12.dp))
    trailing()
}

/** 아이콘 + 제목 + 설명 + 오른쪽 요소. onClick이 있으면 행 전체가 버튼. */
@Composable
fun ListRow(
    icon: Int,
    title: String,
    desc: String? = null,
    tint: Color = C.accent,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val base = Modifier.fillMaxWidth().heightIn(min = 64.dp)
    val clickable = if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base
    Row(clickable.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RowBody(icon, tint, title, desc, trailing)
    }
}

/** 행 전체가 스위치 */
@Composable
fun SwitchRow(icon: Int, title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowBody(icon, C.accent, title, desc) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = C.onAccent, checkedTrackColor = C.accent, checkedBorderColor = C.accent,
                    uncheckedThumbColor = C.muted, uncheckedTrackColor = C.line, uncheckedBorderColor = C.line,
                ),
            )
        }
    }
}

@Composable
fun Chevron() {
    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = C.muted, modifier = Modifier.size(20.dp))
}

/** 아이콘만 있는 작은 버튼 (누르는 영역 48dp) */
@Composable
fun IconAction(icon: Int, description: String, tint: Color = C.muted, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** 둥근 실행 버튼 (▶) */
@Composable
fun RunButton(description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).alpha(if (enabled) 1f else 0.4f).clip(CircleShape).background(C.accentSoft)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = C.accentText, modifier = Modifier.size(18.dp))
    }
}
