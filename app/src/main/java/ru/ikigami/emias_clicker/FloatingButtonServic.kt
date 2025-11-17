package com.example.autoclicker

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var buttonContainer: PassThroughLinearLayout
    private lateinit var statusPanel: PassThroughLinearLayout
    private lateinit var statusTextView: TextView
    private lateinit var logContainer: PassThroughLinearLayout
    private lateinit var logTextView: TextView
    private var isClickerRunning = false
    private val logMessages = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        var instance: FloatingButtonService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // ========== КНОПКА (обёрнута в PassThroughLinearLayout) ==========
        buttonContainer = PassThroughLinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        buttonContainer.addView(floatingView)

        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        windowManager.addView(buttonContainer, buttonParams)

        // ========== ПАНЕЛЬ СТАТУСА (справа от кнопки) ==========
        statusPanel = PassThroughLinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(220, 0, 100, 200))
            setPadding(12, 6, 12, 6)
        }
        statusTextView = TextView(this).apply {
            textSize = 9f
            setTextColor(Color.WHITE)
            text = "⏸️ Пауза"
        }
        statusPanel.addView(statusTextView)

        val statusParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 160  // Справа от кнопки (100 + ~60)
            y = 100  // На одном уровне с кнопкой
        }
        windowManager.addView(statusPanel, statusParams)

        // ========== ЛОГ (30 строк) ==========
        logContainer = PassThroughLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            setPadding(8, 8, 8, 8)
        }
        logTextView = TextView(this).apply {
            textSize = 10f
            setTextColor(Color.WHITE)
            text = "Лог загружается..."
        }
        logContainer.addView(logTextView)

        val logParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 10
            y = 100
        }
        windowManager.addView(logContainer, logParams)

        setupButton(buttonParams, statusParams)
        setupLogDrag(logParams)
        setupLogClickAndLongPress()

        // Сохранить начальные координаты кнопки
        saveButtonCoordinates(buttonParams.x, buttonParams.y)
        addLog("✅ FloatingButton готов (ПАУЗА)")
    }

    private fun setupButton(
        buttonParams: WindowManager.LayoutParams,
        statusParams: WindowManager.LayoutParams
    ) {
        val floatingButton: ImageButton = floatingView.findViewById(R.id.floatingButton)
        floatingButton.setBackgroundResource(android.R.drawable.ic_menu_mylocation)

        floatingButton.setOnClickListener {
            isClickerRunning = !isClickerRunning
            when {
                isClickerRunning && ClickerService.instance == null -> {
                    addLog("❌ Accessibility не включен!")
                    isClickerRunning = false
                    updateStatusPanel("⏸️ Пауза")
                }
                isClickerRunning -> {
                    ClickerService.instance?.startClicking()
                    floatingButton.setBackgroundResource(android.R.drawable.ic_media_pause)
                    updateStatusPanel("🔍 Ищем...")
                    addLog("▶️ Кликер запущен")
                }
                else -> {
                    ClickerService.instance?.stopClicking()
                    floatingButton.setBackgroundResource(android.R.drawable.ic_menu_mylocation)
                    updateStatusPanel("⏸️ Пауза")
                    addLog("⸸️ Кликер на паузе")
                }
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = buttonParams.x
                    initialY = buttonParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    buttonParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    buttonParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(buttonContainer, buttonParams)

                    // Сохраняем координаты в SharedPreferences
                    saveButtonCoordinates(buttonParams.x, buttonParams.y)

                    // Обновляем позицию статус-панели справа от кнопки
                    statusParams.x = buttonParams.x + 60
                    statusParams.y = buttonParams.y
                    windowManager.updateViewLayout(statusPanel, statusParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupLogDrag(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        logContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(logContainer, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupLogClickAndLongPress() {
        logContainer.setOnClickListener {
            // Копируем лог в буфер обмена
            val logText = logMessages.joinToString("\n")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ClickerLog", logText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "📋 Лог скопирован в буфер", Toast.LENGTH_SHORT).show()
        }

        logContainer.setOnLongClickListener {
            logMessages.clear()
            logTextView.text = "📝 Лог очищен"

            // Получить координаты кнопки и вывести их
            val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
            val buttonX = prefs.getInt("buttonX", 100)
            val buttonY = prefs.getInt("buttonY", 100)
            val centerX = buttonX + 28
            val centerY = buttonY + 60

            addLog("🗑️ Лог очищен")
            addLog("📍 Координаты: x=$centerX, y=$centerY")
            true
        }
    }

    fun addLog(message: String) {
        handler.post {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            logMessages.add("$time $message")
            if (logMessages.size > 30) logMessages.removeAt(0)  // 30 строк
            logTextView.text = logMessages.joinToString("\n")
        }
    }

    fun updateStatus(status: String) {
        handler.post {
            statusTextView.text = status
        }
    }

    private fun saveButtonCoordinates(x: Int, y: Int) {
        val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("buttonX", x)
            .putInt("buttonY", y)
            .apply()
    }

    private fun updateStatusPanel(status: String) {
        updateStatus(status)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(buttonContainer)
        windowManager.removeView(statusPanel)
        windowManager.removeView(logContainer)
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}