package app.sureureuk.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.core.PermState
import app.sureureuk.core.Perms
import app.sureureuk.update.Updates

private class PermItem(val title: String, val tag: String, val desc: String, val done: Boolean, val onGrant: () -> Unit)

@Composable
fun PermissionsScreen(perms: PermState, onRefresh: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }

    val items = listOf(
        PermItem("알림 표시", "필수", "남은 시간과 +5분 버튼을 알림에 보여줘요", perms.notifications) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        PermItem("알림 접근", "필수", "재생 중인 앱을 찾아서 멈추는 데 써요", perms.listener) {
            Perms.open(ctx, Perms.listenerIntents(ctx))
        },
        PermItem("알람 및 리마인더", "필수", "정확한 시간에 타이머를 끝내요", perms.exactAlarm) {
            Perms.open(ctx, Perms.exactAlarmIntents(ctx))
        },
        PermItem("배터리 최적화 제외", "권장", "밤새 타이머가 멈추지 않게 해요", perms.battery) {
            Perms.open(ctx, Perms.batteryIntents())
        },
        PermItem("다른 앱 위에 표시", "권장", "영상 위에 남은 시간과 +10분 버튼을 띄워요", perms.overlay) {
            Perms.open(ctx, Perms.overlayIntents(ctx))
        },
        PermItem("시스템 설정 변경", "선택", "화면을 더 빨리 끌 수 있어요", perms.writeSettings) {
            Perms.open(ctx, Perms.writeSettingsIntents(ctx))
        },
    )
    val done = items.count { it.done }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(C.iconBg), contentAlignment = Alignment.Center) {
            MoonIcon(28.dp, C.amber)
        }
        Text("편히 잠들 수 있게\n몇 가지만 허용해 주세요", fontSize = 26.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, color = C.text)
        Text("꼭 필요한 권한만 요청해요. 언제든 설정에서 바꿀 수 있어요.", fontSize = 14.sp, color = C.muted)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("준비 상태", fontSize = 13.sp, color = C.muted, modifier = Modifier.weight(1f))
                Text("$done/${items.size} 완료", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.surface2)) {
                Box(Modifier.fillMaxWidth(done / items.size.toFloat()).height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.accent))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { i, item -> PermRow(i + 1, item) }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface)
                .clickable(role = Role.Button) { Perms.open(ctx, arrayOf(Perms.appDetails(ctx))) }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("'알림 접근'이 회색으로 막혀 있다면", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Text(
                "설치 파일(APK)로 설치한 앱은 Android 13부터 제한돼요. 여기를 눌러 앱 정보 → 오른쪽 위 ⋮ → '제한된 설정 허용'을 먼저 눌러 주세요.",
                fontSize = 12.sp, color = C.muted,
            )
        }

        Text(
            if (Updates.enabled) "스르륵은 새 버전 확인 외에는 인터넷을 쓰지 않고, 어떤 정보도 모으지 않아요."
            else "스르륵은 인터넷을 쓰지 않고, 어떤 정보도 모으지 않아요.",
            fontSize = 13.sp, color = C.mint,
        )
        Spacer(Modifier.height(4.dp))
        PrimaryButton(if (perms.requiredOk) "시작하기" else "필수 권한 없이 둘러보기", onClick = onDone)
    }
}

@Composable
private fun PermRow(n: Int, item: PermItem) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.surface)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberBadge("$n", item.done)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.text)
                val required = item.tag == "필수"
                Text(
                    item.tag, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (required) C.accentText else C.muted,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (required) C.accentSoft else C.surface2)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(item.desc, fontSize = 12.sp, color = C.muted)
        }
        if (item.done) {
            Text("완료", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.mint, modifier = Modifier.padding(horizontal = 10.dp))
        } else {
            SmallOutlineButton("허용", item.onGrant)
        }
    }
}
