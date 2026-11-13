package app.sureureuk.core

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 검증용 기록. 어떤 방법이 어떤 앱에서 통했는지 기기에서 바로 확인하고 공유할 수 있게 남긴다.
 * logcat 태그: Sureureuk
 */
object VerifyLog {
    private const val MAX = 800
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines
    private var file: File? = null
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)

    fun init(context: Context) {
        val f = File(context.filesDir, "verify.log")
        file = f
        if (f.exists()) {
            _lines.value = f.readLines().takeLast(MAX)
            if (f.length() > 512_000) f.writeText(_lines.value.joinToString("\n", postfix = "\n"))
        }
    }

    @Synchronized
    fun i(tag: String, msg: String) {
        val line = "${fmt.format(Date())} [$tag] $msg"
        Log.i("Sureureuk", line)
        _lines.value = (_lines.value + line).takeLast(MAX)
        try {
            file?.appendText(line + "\n")
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun clear() {
        _lines.value = emptyList()
        try {
            file?.writeText("")
        } catch (_: Exception) {
        }
    }

    fun deviceInfo(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun export(): String = "스르륵 검증 기록\n${deviceInfo()}\n\n" + _lines.value.joinToString("\n")
}
