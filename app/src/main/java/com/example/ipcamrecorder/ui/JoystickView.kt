package com.example.ipcamrecorder.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Very simple joystick view.
 * Emits normalized (-1..1) x and y on touch via listener.
 */
class JoystickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paintBg = Paint().apply { color = Color.argb(80, 0, 0, 0); style = Paint.Style.FILL }
    private val paintHandle = Paint().apply { color = Color.DKGRAY; style = Paint.Style.FILL }
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var handleRadius = 0f
    private var handleX = 0f
    private var handleY = 0f

    private var listener: ((x: Float, y: Float, magnitude: Float) -> Unit)? = null

    fun setOnMoveListener(l: (x: Float, y: Float, magnitude: Float) -> Unit) {
        listener = l
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = Math.min(w, h) * 0.42f
        handleRadius = baseRadius * 0.4f
        resetHandle()
    }

    private fun resetHandle() {
        handleX = centerX
        handleY = centerY
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // base
        canvas.drawCircle(centerX, centerY, baseRadius, paintBg)
        // handle
        canvas.drawCircle(handleX, handleY, handleRadius, paintHandle)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = hypot(dx, dy)
                val max = baseRadius - handleRadius
                if (dist > max) {
                    val ratio = max / dist
                    handleX = centerX + dx * ratio
                    handleY = centerY + dy * ratio
                } else {
                    handleX = event.x
                    handleY = event.y
                }
                val nx = (handleX - centerX) / max
                val ny = (handleY - centerY) / max
                val mag = hypot(nx, ny).coerceAtMost(1.0f)
                listener?.invoke(nx, -ny, mag) // invert Y so up is positive
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetHandle()
                listener?.invoke(0f, 0f, 0f)
            }
        }
        return true
    }
}
