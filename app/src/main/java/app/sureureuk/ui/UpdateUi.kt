package app.sureureuk.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.R
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
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(C.accentSoft)
            .clickable(role = Role.Button) { openUrl(ctx, r.pageUrl) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = C.accentText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("새 버전 ${r.version}".keepAll(), fontSize = 14.sp, color = C.text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text("받기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.accentText, maxLines = 1, softWrap = false)
    }
}

/** 설정 '앱' 묶음에 이어 붙는 행: 업데이트 확인, 변경 내역 (github 배포판만) */
@Composable
fun UpdateRows() {
    if (!Updates.enabled) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rel by Updates.latest.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    RowDivider()
    val r = rel
    if (r != null) {
        ListRow(R.drawable.ic_download, "새 버전 ${r.version}", "눌러서 받으러 가요", tint = C.amber, onClick = { openUrl(ctx, r.pageUrl) }) { Chevron() }
    } else {
        ListRow(
            R.drawable.ic_download, "업데이트 확인",
            if (checking) "확인하는 중이에요" else status ?: "지금 ${Updates.currentVersion} 버전을 쓰고 있어요",
            onClick = {
                if (!checking) {
                    checking = true
                    scope.launch {
                        status = when (Updates.check(ctx, force = true)) {
                            Updates.Result.LATEST -> "최신 버전이에요"
                            Updates.Result.FAILED -> "확인하지 못했어요. 인터넷 연결을 확인해 주세요"
                            else -> null
                        }
                        checking = false
                    }
                }
            },
        )
    }
    RowDivider()
    ListRow(R.drawable.ic_doc, "변경 내역", onClick = { openUrl(ctx, Updates.releasesPage) }) { Chevron() }
}
