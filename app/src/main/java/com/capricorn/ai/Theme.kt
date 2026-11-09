package com.capricorn.ai

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView

object Theme {
    const val CYAN = 0xFF00E5FF.toInt()
    const val PINK = 0xFFFF2E63.toInt()
    const val YELLOW = 0xFFFFE600.toInt()
    const val BG = 0xFF05070D.toInt()
    const val PANEL = 0xFF0B101B.toInt()
    const val TEXT = 0xFFE6F7FF.toInt()
    const val MUTED = 0xFF78909C.toInt()

    fun dp(c: Context, value: Int): Int = (value * c.resources.displayMetrics.density).toInt()

    fun panel(c: Context, stroke: Int = CYAN, fill: Int = PANEL): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            setStroke(dp(c, 1), stroke)
            cornerRadius = dp(c, 8).toFloat()
        }

    fun chip(c: Context, text: String, color: Int = CYAN): TextView = TextView(c).apply {
        this.text = text
        setTextColor(color)
        textSize = 13f
        typeface = Typeface.create("monospace", Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10))
        background = panel(c, color)
        isClickable = true
        isFocusable = true
    }
}
