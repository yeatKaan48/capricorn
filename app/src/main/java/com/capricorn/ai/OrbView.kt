package com.capricorn.ai

import android.content.Context
import android.graphics.*
import android.view.View
import java.io.File
import kotlin.math.min
import kotlin.math.sin

class OrbView(c: Context) : View(c) {
    var state = 0
    var level = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sp = Paint()
    private val rect = RectF()
    private var smooth = 0f
    private val frames = arrayOfNulls<List<Bitmap>>(4)
    private val movies = arrayOfNulls<Movie>(4)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        reload()
    }

    fun reload() {
        for (s in 0..3) {
            frames[s] = null
            movies[s] = null
            val files = File(context.filesDir, "sprites/$s").listFiles()?.sortedBy { it.name } ?: continue
            if (files.isEmpty()) continue
            if (files.size == 1 && files[0].name.endsWith(".gif", true)) {
                movies[s] = Movie.decodeFile(files[0].path)
            } else {
                frames[s] = files.mapNotNull { decode(it.path) }
            }
        }
    }

    private fun decode(path: String): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var k = 1
        while (o.outWidth / k > 400) k *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = k })
    }

    private fun has(s: Int) = movies[s] != null || !frames[s].isNullOrEmpty()

    private fun drawSprite(cv: Canvas, s: Int) {
        val mv = movies[s]
        if (mv != null) {
            val d = if (mv.duration() == 0) 1000 else mv.duration()
            mv.setTime((System.currentTimeMillis() % d).toInt())
            val sc = min(width / mv.width().toFloat(), height / mv.height().toFloat())
            cv.save()
            cv.scale(sc, sc)
            mv.draw(cv, (width / sc - mv.width()) / 2f, (height / sc - mv.height()) / 2f)
            cv.restore()
            return
        }
        val fr = frames[s] ?: return
        val b = fr[((System.currentTimeMillis() / 125) % fr.size).toInt()]
        val sc = min(width / b.width.toFloat(), height / b.height.toFloat())
        val w = b.width * sc
        val h = b.height * sc
        rect.set((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
        cv.drawBitmap(b, null, rect, sp)
    }

    override fun onDraw(cv: Canvas) {
        val sprite = if (has(state)) state else if (has(0)) 0 else -1
        if (sprite >= 0) {
            drawSprite(cv, sprite)
            postInvalidateDelayed(40)
            return
        }
        val t = System.currentTimeMillis() / 1000f
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) * 0.55f
        smooth += (level - smooth) * 0.25f
        val col = when (state) {
            1 -> 0xFFFF2E63.toInt()
            2 -> 0xFFFFE600.toInt()
            3 -> 0xFF00E5FF.toInt()
            else -> 0xFF00B8CC.toInt()
        }
        p.style = Paint.Style.FILL
        p.color = 0xE60B0F1A.toInt()
        cv.drawCircle(cx, cy, r, p)
        val pulse = when (state) {
            1 -> smooth
            3 -> (sin(t * 6f) + 1f) / 4f
            else -> (sin(t * 2f) + 1f) / 10f
        }
        p.style = Paint.Style.STROKE
        p.color = col
        p.strokeWidth = r * 0.08f
        cv.drawCircle(cx, cy, r * (1f + pulse * 0.35f), p)
        p.alpha = 90
        cv.drawCircle(cx, cy, r * (1.15f + pulse * 0.5f), p)
        p.alpha = 255
        if (state == 2) {
            p.strokeWidth = r * 0.1f
            rect.set(cx - r * 0.8f, cy - r * 0.8f, cx + r * 0.8f, cy + r * 0.8f)
            cv.drawArc(rect, (t * 360f) % 360f, 100f, false, p)
        }
        p.style = Paint.Style.FILL
        p.color = col
        p.alpha = 200
        cv.drawCircle(cx, cy, r * (0.28f + (if (state == 1) smooth * 0.2f else 0f)), p)
        p.alpha = 255
        postInvalidateDelayed(33)
    }
}
