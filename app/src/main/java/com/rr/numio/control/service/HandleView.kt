package com.rr.numio.control.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * The thin amber handle on the screen edge. Swipe in from it, or tap it, to open the
 * panel. TalkBack reads it as a button ("Open Control panel").
 */
@SuppressLint("ViewConstructor")
class HandleView(
    context: Context,
    private val onRightEdge: Boolean,
    private val onOpen: () -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val swipeDistance = 20 * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8CFFC21A.toInt() }
    private val bar = RectF()
    private var downX = 0f
    private var downY = 0f
    private var opened = false

    init {
        contentDescription = "Open Control panel"
        isClickable = true
        isFocusable = true
        setOnClickListener { onOpen() }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Ask Android not to treat swipes here as its back gesture
        systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
    }

    override fun onDraw(canvas: Canvas) {
        val w = 4 * density
        val inset = 4 * density
        val h = height * 0.6f
        val top = (height - h) / 2
        val left = if (onRightEdge) width - inset - w else inset
        bar.set(left, top, left + w, top + h)
        canvas.drawRoundRect(bar, w, w, paint)
    }

    @SuppressLint("ClickableViewAccessibility") // performClick() is called for taps below
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                opened = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val inward = if (onRightEdge) -dx else dx
                if (!opened && (inward > swipeDistance || abs(event.rawY - downY) > swipeDistance * 2)) {
                    opened = true
                    onOpen()
                }
            }
            MotionEvent.ACTION_UP -> if (!opened) performClick()
        }
        return true
    }
}
