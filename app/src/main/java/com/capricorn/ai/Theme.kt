package com.capricorn.ai

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView

object Theme {
    const val CYAN = 0xFFFF2E70.toInt()
    const val PINK = 0xFFFF2E70.toInt()
    const val YELLOW = 0xFFF4B8CC.toInt()
    const val BG = 0xFF08060B.toInt()
    const val PANEL = 0xFF190912.toInt()
    const val TEXT = 0xFFF4DCE5.toInt()
    const val MUTED = 0xFFAA8292.toInt()
    const val BORDER = 0xFF65203C.toInt()
    const val PANEL_DARK = 0xFF0D070C.toInt()

    fun dp(c: Context, value: Int): Int =
        (value * c.resources.displayMetrics.density).toInt()

    fun panel(
        c: Context,
        stroke: Int = BORDER,
        fill: Int = PANEL
    ): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        setStroke(dp(c, 1), stroke)
        cornerRadius = dp(c, 16).toFloat()
    }

    fun gradientPanel(
        c: Context,
        stroke: Int = BORDER,
        top: Int = PANEL,
        bottom: Int = PANEL_DARK
    ): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(top, bottom)
    ).apply {
        setStroke(dp(c, 1), stroke)
        cornerRadius = dp(c, 16).toFloat()
    }

    fun chip(
        c: Context,
        text: String,
        color: Int = CYAN
    ): TextView = TextView(c).apply {
        this.text = text
        setTextColor(color)
        textSize = 11f
        letterSpacing = 0.08f
        typeface = Typeface.create("monospace", Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(c, 15), dp(c, 10), dp(c, 15), dp(c, 10))
        background = gradientPanel(
            c,
            color,
            0xFF21101A.toInt(),
            0xFF120A10.toInt()
        )
        isClickable = true
        isFocusable = true
        elevation = dp(c, 3).toFloat()
    }
}
