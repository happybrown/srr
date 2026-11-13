package app.sureureuk.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import app.sureureuk.core.VerifyLog

/**
 * 취침 화면: 화면 전체를 덮는 불투명한 검은 창.
 *
 * 넷플릭스처럼 일시정지 상태에서도 '화면 켜두기(FLAG_KEEP_SCREEN_ON)'를 유지하는 앱이 있으면
 * 화면 꺼짐 시간이 지나도 화면이 꺼지지 않는다. 안드로이드는 다른 불투명 전체 화면 창에
 * 완전히 가려진 창의 '화면 켜두기'는 무시하므로, 이 창으로 덮으면 시스템이 화면을 끌 수 있다.
 * 창 밝기를 최저로 지정해 꺼지기 전에도 바로 어두워진다.
 * '다른 앱 위에 표시' 권한이 필요하다.
 */
class SleepCurtain(private val ctx: Context) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private var view: View? = null

    val isShowing: Boolean get() = view != null

    fun show(onTouch: () -> Unit): Boolean {
        if (view != null) return true
        if (!Settings.canDrawOverlays(ctx)) return false

        val root = FrameLayout(ctx).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            contentDescription = "취침 화면. 누르면 닫혀요"
            setOnClickListener { onTouch() }
            addView(
                TextView(ctx).apply {
                    text = "잘 자요"
                    textSize = 16f
                    setTextColor(0xFF2E3248.toInt())
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE, // 불투명해야 아래 앱을 '가린' 것으로 인정된다
        ).apply {
            screenBrightness = 0f // 이 창이 보이는 동안 최저 밝기
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        return try {
            wm.addView(root, lp)
            view = root
            true
        } catch (e: Exception) {
            VerifyLog.i("화면", "취침 화면 표시 실패: ${e.javaClass.simpleName}")
            false
        }
    }

    fun hide() {
        view?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Exception) {
            }
        }
        view = null
    }
}
