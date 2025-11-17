package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class ClickerService : AccessibilityService() {

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null

    companion object {
        var instance: ClickerService? = null
        private const val TAG = "ClickerService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service Connected")
        FloatingButtonService.instance?.addLog("✅ Accessibility подключен")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    fun startClicking() {
        isRunning = true
        val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        val buttonX = prefs.getInt("buttonX", 100)
        val buttonY = prefs.getInt("buttonY", 100)
        val centerX = buttonX + 28
        val centerY = buttonY + 60

        FloatingButtonService.instance?.addLog("📄 Запуск цикла...")
        FloatingButtonService.instance?.addLog("📍 Целевые координаты: x=$centerX, y=$centerY")
        clickCycle()
    }

    fun stopClicking() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        stopAlarm()
        FloatingButtonService.instance?.addLog("⹸️ Остановлено")
    }

    private fun clickCycle() {
        if (!isRunning) return

        val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        val searchText = prefs.getString("searchText", "Бабай") ?: "Бабай"
        val negative = prefs.getBoolean("negative", false)

        // 1. АНАЛИЗ
        FloatingButtonService.instance?.updateStatus("🔍 Ищем...")
        val screenText = readScreenText()

        val found = screenText.lowercase(Locale.getDefault())
            .contains(searchText.lowercase(Locale.getDefault()))
        val match = if (negative) !found else found

        FloatingButtonService.instance?.addLog(
            "📖 ${screenText.take(30)}..."
        )
        FloatingButtonService.instance?.addLog(
            "🔍 '$searchText' НЕ=$negative Найдено=$found"
        )

        if (match) {
            // ЗВУК И СТОП
            playAlarm()
            isRunning = false
            FloatingButtonService.instance?.addLog("🎯 СОВПАДЕНИЕ! Остановка")
        } else {
            // СВАЙП НАЗАД
            performSwipeBack()
            FloatingButtonService.instance?.addLog("👈 Свайп назад")

            // Ждём 1000мс
            handler.postDelayed({
                // КЛИК ПО КНОПКЕ (по текущим координатам)
                val buttonX = prefs.getInt("buttonX", 100)
                val buttonY = prefs.getInt("buttonY", 100)
                val clickX = (buttonX + 28).toFloat()  // центр кнопки (56px / 2 = 28)
                val clickY = (buttonY + 60).toFloat()  // чуть ниже центра

                FloatingButtonService.instance?.addLog("🖱️ Клик x=${clickX.toInt()}, y=${clickY.toInt()}")
                performClick(clickX, clickY)

                val delay = prefs.getLong("delay", 500)
                handler.postDelayed({ clickCycle() }, delay)
            }, 1000)
        }
    }

    private fun performClick(x: Float, y: Float) {
        val clickPath = Path().apply {
            moveTo(x, y)
            lineTo(x, y + 1)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(clickPath, 0, 50))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Click completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Click cancelled")
            }
        }, null)
    }

    private fun performSwipeBack() {
        val displayMetrics = resources.displayMetrics
        val swipePath = Path().apply {
            moveTo(50f, displayMetrics.heightPixels / 2f)
            lineTo(displayMetrics.widthPixels * 0.8f, displayMetrics.heightPixels / 2f)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, 300))

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun readScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()
        extractText(rootNode, textBuilder)
        rootNode.recycle()
        return textBuilder.toString()
    }

    private fun extractText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let { builder.append(it).append(" ") }
        node.contentDescription?.let { builder.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                extractText(it, builder)
                it.recycle()
            }
        }
    }

    private fun playAlarm() {
        try {
            stopAlarm()  // остановить старый если был

            ringtone = RingtoneManager.getRingtone(
                this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ).apply {
                play()
            }
            Log.d(TAG, "Alarm started")
            FloatingButtonService.instance?.addLog("🔊 Звук включен")
        } catch (e: Exception) {
            Log.e(TAG, "Alarm error", e)
            FloatingButtonService.instance?.addLog("❌ Ошибка звука: ${e.message}")
        }
    }

    private fun stopAlarm() {
        try {
            ringtone?.stop()
            ringtone = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClicking()
        instance = null
    }
}