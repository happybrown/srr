package app.sureureuk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sureureuk.R

/*
 * 알림·영상 위 표시의 예상 모습 (문서용 그림).
 * 실제 알림은 시스템이 그리므로 기기에서만 볼 수 있다. 여기서는 SleepTimerService와 FadeOverlay에
 * 들어 있는 문구·버튼·막대를 그대로 옮겨 그린다. 알림창의 틀은 기종마다 조금 다르다.
 */

private val Shade = Color(0xFF101012)
private val Card = Color(0xFF26262A)
private val Sub = Color(0xFFB4B4BC)
private val Tint = Color(0xFFA99BFF)

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Sub, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun NotificationCard(
    timer: String,
    title: String,
    text: String,
    remaining: Float?,
    actions: List<String>,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Tint), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_moon), contentDescription = null, tint = Color(0xFF14122B), modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("스르륵", fontSize = 12.sp, color = Sub)
            Text("  ·  $timer", fontSize = 12.sp, color = Sub)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(text, fontSize = 13.sp, color = Sub)
        if (remaining != null) {
            Spacer(Modifier.height(10.dp))
            // 남은 시간: 가득 찬 막대가 줄어든다
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF45454C))) {
                Box(Modifier.fillMaxWidth(remaining).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Tint))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            actions.forEach { Text(it, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Tint) }
        }
    }
}

/** 영상 위 표시 (FadeOverlay): 원형 진행 표시 + 남은 초 + 버튼 두 개 */
@Composable
private fun OverlayBar(seconds: Int, fraction: Float) {
    Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF05060B))) {
        Text("재생 중인 영상", fontSize = 12.sp, color = Color(0xFF55596F), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xF2141628))
                .border(1.dp, Color(0xFF2A2F4D), RoundedCornerShape(28.dp)).padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(22.dp)) {
                val inset = 2.dp.toPx()
                val arc = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(Color(0xFF2A2F4D), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(3.dp.toPx()))
                drawArc(Color(0xFFFFD58A), -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            Spacer(Modifier.width(10.dp))
            Text("${seconds}초", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD5D8EA))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Tint).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text("+10분", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14122B))
            }
            Box(Modifier.height(44.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text("끄기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3C7))
            }
        }
    }
}

@Composable
fun NotificationMock() {
    Column(
        Modifier.fillMaxSize().background(Shade).padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Caption("1. 실행 중 (30분 타이머, 24분 남음)")
        NotificationCard("24:12", "오후 11:30에 잠들어요", "끝나면 소리를 줄이고 화면을 꺼요", 0.80f, listOf("+5분", "+15분", "끄기"))

        Caption("2. 시간이 지나면 막대가 줄어들어요 (6분 남음)")
        NotificationCard("06:03", "오후 11:30에 잠들어요", "끝나면 소리를 줄이고 화면을 꺼요", 0.20f, listOf("+5분", "+15분", "끄기"))

        Caption("3. 마지막 1분: 소리 줄이는 중")
        NotificationCard("00:42", "오후 11:30에 잠들어요", "소리를 줄이는 중", 0.023f, listOf("+5분", "+15분", "끄기"))

        Caption("4. 마지막 1분에 뜨는 팝업 알림 (오버레이 권한이 없을 때)")
        NotificationCard("00:42", "곧 잠들어요", "소리를 줄이는 중이에요", null, listOf("+10분", "끄기"))

        Caption("5. 영상 위 표시 (오버레이 권한이 있을 때)")
        OverlayBar(42, 0.7f)
    }
}
