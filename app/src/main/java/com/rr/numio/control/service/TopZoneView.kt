package com.rr.numio.control.service

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View

/**
 * An invisible strip over the status bar. A swipe down that starts here opens the
 * panel instead of Samsung's. TalkBack reads it as a button ("Open Control panel").
 */
@SuppressLint("ViewConstructor")
class TopZoneView(context: Context, private val onOpen: () -> Unit) : View(context) {

    private val swipeDistance = 16 * resources.displayMetrics.density
    private var downY = 0f
    private var opened = false

    init {
        contentDescription = "Open Control panel"
        isClickable = true
        setOnClickListener { onOpen() }
    }

    @SuppressLint("ClickableViewAccessibility") // performClick() is called for taps below
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.rawY
                opened = false
            }
            MotionEvent.ACTION_MOVE -> if (!opened && event.rawY - downY > swipeDistance) {
                opened = true
                onOpen()
            }
            MotionEvent.ACTION_UP -> if (!opened) performClick()
        }
        return true
    }
}
