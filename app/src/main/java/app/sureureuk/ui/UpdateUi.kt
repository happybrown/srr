package app.sureureuk.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.BuildConfig
import app.sureureuk.update.Updates
import kotlinx.coroutines.launch

/**
 * 새 버전은 브라우저에서 릴리스 페이지를 열어 사용자가 직접 받는다.
 * (앱이 APK를 내려받아 설치하면 Play 프로텍트가 '드로퍼' 앱으로 오인할 수 있어 하지 않는다)
 */
private fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

/** 홈 상단: 새 버전이 있을 때만 보인다. */
@Composable
fun UpdateBanner() {
    val ctx = LocalContext.current
    val rel by Updates.latest.collectAsState()
    val r = rel ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.accentSoft)
            .clickable(role = Role.Button) { openUrl(ctx, r.pageUrl) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("새 버전 ${r.version}이 나왔어요", fontSize = 13.sp, color = C.text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("받으러 가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.accentText)
    }
}

/** 설정 하단: 버전 정보와 수동 업데이트 확인 */
@Composable
fun AppInfoCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rel by Updates.latest.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    AppCard {
        Text("앱 정보", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.muted)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("스르륵 ${Updates.currentVersion}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Text(
                if (Updates.enabled) "GitHub 배포판 · 새 버전 확인 외에는 인터넷을 쓰지 않아요"
                else "플레이 스토어판 · 업데이트는 플레이 스토어에서 받아요",
                fontSize = 12.sp, color = C.muted,
            )
        }
        if (Updates.enabled) {
            val r = rel
            if (r != null) {
                PrimaryButton("${r.version} 받으러 가기") { openUrl(ctx, r.pageUrl) }
            } else {
                SecondaryButton(if (checking) "확인 중…" else "업데이트 확인", Modifier.fillMaxWidth(), enabled = !checking, height = 48.dp) {
                    checking = true
                    scope.launch {
                        status = when (Updates.check(ctx, force = true)) {
                            Updates.Result.UPDATE -> null
                            Updates.Result.LATEST -> "최신 버전이에요"
                            Updates.Result.FAILED -> "확인하지 못했어요. 인터넷 연결을 확인해 주세요"
                            else -> null
                        }
                        checking = false
                    }
                }
            }
            status?.let { Text(it, fontSize = 13.sp, color = C.muted) }
            Text(
                "변경 내역 보기",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.accent,
                modifier = Modifier.clickable(role = Role.Button) { openUrl(ctx, Updates.releasesPage) }
                    .padding(vertical = 8.dp),
            )
        }
        if (BuildConfig.DEBUG) Text("개발용 빌드 (${BuildConfig.FLAVOR})", fontSize = 12.sp, color = C.muted)
    }
}
