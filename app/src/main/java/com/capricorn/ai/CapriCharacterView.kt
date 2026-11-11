package com.capricorn.ai

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView

class CapriCharacterView(context: Context) : ImageView(context) {

    companion object {
        const val IDLE = 0
        const val THINKING = 1
        const val LISTENING = 2
        const val SPEAKING = 3
        const val REACTION = 4
    }

    private val spriteFiles = mapOf(
        IDLE to "sleep.webp",
        THINKING to "chatting.gif",
        LISTENING to "voice.gif",
        SPEAKING to "voice.gif",
        REACTION to "tossed.webp"
    )

    var state: Int = IDLE
        private set

    init {
        scaleType = ScaleType.FIT_CENTER
        adjustViewBounds = true
        contentDescription = "Capri karakteri"
        loadSprite(IDLE)
    }

    fun setState(newState: Int) {
        val safeState = newState.coerceIn(IDLE, REACTION)
        if (state == safeState && drawable != null) return
        state = safeState
        loadSprite(safeState)
    }

    private fun loadSprite(targetState: Int) {
        val fileName = spriteFiles[targetState] ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(
                    context.assets,
                    "sprites/$fileName"
                )
                (drawable as? AnimatedImageDrawable)?.stop()

                val image = ImageDecoder.decodeDrawable(source)
                setImageDrawable(image)
                visibility = View.VISIBLE

                if (image is AnimatedImageDrawable) {
                    image.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    image.start()
                }
            }
        } catch (error: Exception) {
            setImageDrawable(null)
            visibility = View.INVISIBLE
            android.util.Log.e(
                "CapriSprite",
                "Sprite yüklenemedi: $fileName",
                error
            )
        }
    }

    fun stopAnimation() {
        (drawable as? AnimatedImageDrawable)?.stop()
    }

    fun resumeAnimation() {
        (drawable as? AnimatedImageDrawable)?.start()
    }
}
