package app.sureureuk.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.sureureuk.core.VerifyLog
import kotlin.math.roundToInt

/**
 * 마지막 1분 동안 넷플릭스·유튜브 화면 위에 뜨는 작은 막대.
 * '다른 앱 위에 표시' 권한이 필요하다. 없으면 서비스가 팝업 알림으로 대신한다.
 */
class FadeOverlay(
    private val ctx: Context,
    private val onExtend: () -> Unit,
    private val onStopNow: () -> Unit,
) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private var root: View? = null
    private var secsView: TextView? = null
    private var ringView: RingView? = null

    private fun px(dp: Int): Int = (dp * ctx.resources.displayMetrics.density).roundToInt()

    fun canShow(): Boolean = Settings.canDrawOverlays(ctx)

    fun show(): Boolean {
        if (root != null) return true
        if (!canShow()) return false

        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16), px(6), px(6), px(6))
            background = GradientDrawable().apply {
                setColor(0xF2141628.toInt())
                cornerRadius = px(28).toFloat()
                setStroke(px(1), 0xFF2A2F4D.toInt())
            }
        }
        val title = TextView(ctx).apply {
            text = "소리를 줄이는 중"
            setTextColor(0xFFD5D8EA.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        val secs = TextView(ctx).apply {
            setTextColor(0xFFD5D8EA.toInt())
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(px(10), 0, px(6), 0)
        }
        val ring = RingView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(px(22), px(22)).apply { marginEnd = px(10) }
        }
        bar.addView(ring)
        bar.addView(title)
        bar.addView(secs)
        bar.addView(pill("+10분", filled = true) { onExtend() })
        bar.addView(pill("끄기", filled = false) { onStopNow() })

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = px(16)
        }
        return try {
            wm.addView(bar, lp)
            root = bar
            secsView = secs
            ringView = ring
            true
        } catch (e: Exception) {
            VerifyLog.i("오버레이", "표시 실패: ${e.javaClass.simpleName}")
            false
        }
    }

    private fun pill(label: String, filled: Boolean, onClick: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            stateListAnimator = null
            minHeight = px(44)
            minimumHeight = px(44)
            minWidth = px(44)
            minimumWidth = px(44)
            setPadding(px(16), 0, px(16), 0)
            setTextColor(if (filled) 0xFF14122B.toInt() else 0xFF9CA3C7.toInt())
            background = if (filled) {
                GradientDrawable().apply {
                    setColor(0xFFA99BFF.toInt())
                    cornerRadius = px(22).toFloat()
                }
            } else {
                null
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, px(44)).apply {
                marginStart = px(6)
            }
        }

    fun update(secondsLeft: Int, totalSeconds: Int) {
        secsView?.text = "${secondsLeft}초"
        ringView?.fraction = (secondsLeft.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    fun hide() {
        root?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Exception) {
            }
        }
        root = null
        secsView = null
        ringView = null
    }

    /** 남은 비율만큼 채워지는 작은 원 (12시부터 시계 방향) */
    private class RingView(ctx: Context) : View(ctx) {
        var fraction = 1f
            set(v) {
                field = v
                invalidate()
            }
        private val density = ctx.resources.displayMetrics.density
        private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3 * density; color = 0xFF2A2F4D.toInt()
        }
        private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3 * density; color = 0xFFFFD58A.toInt(); strokeCap = Paint.Cap.ROUND
        }
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            val inset = 2 * density
            rect.set(inset, inset, width - inset, height - inset)
            canvas.drawOval(rect, track)
            canvas.drawArc(rect, -90f, 360f * fraction, false, arc)
        }
    }
}
