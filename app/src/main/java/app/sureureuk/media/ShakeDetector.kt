package app.sureureuk.media

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/** 휴대폰을 두 번 흔들면 onShake. 화면을 켜지 않고 연장하기 위한 용도. */
class ShakeDetector(ctx: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sm = ctx.getSystemService(SensorManager::class.java)
    private var running = false
    private var lastPeak = 0L
    private var peaks = 0
    private var lastShake = 0L

    fun start() {
        if (running) return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        running = sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!running) return
        sm.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g < 2.5f) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastShake < 3_000) return
        peaks = if (now - lastPeak < 600) peaks + 1 else 1
        lastPeak = now
        if (peaks >= 2) {
            peaks = 0
            lastShake = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
