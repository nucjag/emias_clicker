package com.example.autoclicker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import kotlin.math.abs

class PassThroughLinearLayout(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var lastClickTime = 0L
    private val LONG_PRESS_DURATION = 500L
    private val DRAG_TOLERANCE = 20  // пиксели
    private val DOUBLE_CLICK_TIME = 300L  // миллисекунды

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                downX = event.x
                downY = event.y
                super.dispatchTouchEvent(event)
            }
            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - downTime
                val deltaX = abs(event.x - downX)
                val deltaY = abs(event.y - downY)
                val distance = kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toInt()

                // Long press с tolerance (движение < 20px)
                if (duration >= LONG_PRESS_DURATION && distance < DRAG_TOLERANCE) {
                    performLongClick()
                }
                // Double-click (два клика за 300ms)
                else if (duration < 200 && distance < DRAG_TOLERANCE) {
                    val timeSinceLastClick = System.currentTimeMillis() - lastClickTime
                    if (timeSinceLastClick in 100..DOUBLE_CLICK_TIME) {
                        // Double-click! Очищаем
                        performLongClick()
                        lastClickTime = 0  // Сброс
                    } else {
                        // Первый клик в паре
                        lastClickTime = System.currentTimeMillis()
                        performClick()
                    }
                }

                super.dispatchTouchEvent(event)
            }
            else -> super.dispatchTouchEvent(event)
        }

        // НЕ перехватываем - пропускаем дальше для лончера
        return false
    }
}