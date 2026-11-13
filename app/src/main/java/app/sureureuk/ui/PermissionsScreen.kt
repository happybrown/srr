package app.sureureuk.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.BuildConfig
import app.sureureuk.R
import app.sureureuk.core.PermState
import app.sureureuk.core.Perms
import app.sureureuk.update.Updates

private class PermItem(val icon: Int, val title: String, val desc: String, val done: Boolean, val onGrant: () -> Unit)

/**
 * 처음 실행 권한 안내. 필수 3개를 먼저 보여 주고 나머지는 '선택'으로 나눈다.
 * 하단 고정 버튼이 다음에 허용할 필수 권한으로 바로 데려간다.
 */
@Composable
fun PermissionsScreen(perms: PermState, onRefresh: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }

    val required = listOf(
        PermItem(R.drawable.ic_bell, "알림 표시", "남은 시간과 연장 버튼을 알림에 보여줘요", perms.notifications) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        PermItem(R.drawable.ic_music, "알림 접근", "재생 중인 앱을 찾아서 멈춰요. 알림 내용은 읽지 않아요", perms.listener) {
            Perms.open(ctx, Perms.listenerIntents(ctx))
        },
        PermItem(R.drawable.ic_alarm, "알람 및 리마인더", "휴대폰이 잠들어 있어도 정확한 시간에 끝내요", perms.exactAlarm) {
            Perms.open(ctx, Perms.exactAlarmIntents(ctx))
        },
    )
    val optional = listOfNotNull(
        PermItem(R.drawable.ic_layers, "다른 앱 위에 표시", "영상 앱이 화면을 켜 둬도 끌 수 있고, 영상 위에 +10분 버튼을 띄워요", perms.overlay) {
            Perms.open(ctx, Perms.overlayIntents(ctx))
        },
        if (BuildConfig.A11Y_LOCK) {
            PermItem(R.drawable.ic_person, "접근성: 바로 잠그기", "끝나면 전원 버튼처럼 바로 꺼요. 화면 내용은 읽지 않아요", perms.accessibility) {
                Perms.open(ctx, Perms.accessibilityIntents())
            }
        } else {
            null
        },
        PermItem(R.drawable.ic_battery, "배터리 최적화 제외", "밤새 타이머가 멈추지 않게 해요", perms.battery) {
            Perms.open(ctx, Perms.batteryIntents())
        },
        PermItem(R.drawable.ic_sun, "시스템 설정 변경", "화면이 10초 만에 꺼져요", perms.writeSettings) {
            Perms.open(ctx, Perms.writeSettingsIntents(ctx))
        },
    )
    val doneCount = required.count { it.done }
    val next = required.firstOrNull { !it.done }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.iconBg), contentAlignment = Alignment.Center) {
                MoonIcon(28.dp, C.amber)
            }
            Text(
                "잠들기 전에\n${required.size}가지만 허용해 주세요".keepAll(),
                fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = C.text,
                modifier = Modifier.semantics { heading() },
            )
            Text("나머지는 선택이에요. 언제든 설정에서 바꿀 수 있어요.".keepAll(), fontSize = 15.sp, color = C.muted)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("필수 ${required.size}개 중 ${doneCount}개 완료", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.text)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.surface2)) {
                    Box(
                        Modifier.fillMaxWidth(doneCount / required.size.toFloat()).height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(C.accent),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                required.forEach { PermRow(it) }
            }

            Text(
                "더 잘 되게 하려면 (선택)",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                optional.forEach { PermRow(it) }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface)
                    .clickable(role = Role.Button) { Perms.open(ctx, arrayOf(Perms.appDetails(ctx))) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("'알림 접근'이나 '접근성'이 회색으로 막혀 있다면".keepAll(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.text)
                Text(
                    "파일로 받아 설치한 앱은 Android 13부터 제한돼요. 여기를 눌러 앱 정보 → 오른쪽 위 ⋮ → '제한된 설정 허용'을 먼저 눌러 주세요.".keepAll(),
                    fontSize = 13.sp, color = C.muted,
                )
            }

            Text(
                (if (Updates.enabled) "스르륵은 새 버전 확인 외에는 인터넷을 쓰지 않고, 어떤 정보도 모으지 않아요."
                else "스르륵은 인터넷을 쓰지 않고, 어떤 정보도 모으지 않아요.").keepAll(),
                fontSize = 13.sp, color = C.mint,
            )
        }

        // 하단 고정: 다음 필수 권한으로 바로 / 다 됐으면 시작
        Column(
            Modifier.fillMaxWidth().background(C.bg).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (next == null) {
                PrimaryButton("시작하기", onClick = onDone)
            } else {
                PrimaryButton("다음: ${next.title} 허용하기", onClick = next.onGrant)
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onDone),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("나중에 할게요", fontSize = 14.sp, color = C.muted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun PermRow(item: PermItem) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(item.icon, tint = if (item.done) C.mint else C.accent, done = item.done)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title.keepAll(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Text(item.desc.keepAll(), fontSize = 13.sp, color = C.muted)
        }
        if (item.done) {
            Text("완료", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.mint, modifier = Modifier.padding(horizontal = 8.dp))
        } else {
            SmallOutlineButton("허용하기", description = "${item.title} 허용하기", onClick = item.onGrant)
        }
    }
}
