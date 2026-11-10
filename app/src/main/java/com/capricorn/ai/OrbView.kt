package com.capricorn.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import java.io.File
import kotlin.math.min
import kotlin.math.sin

class OrbView(context: Context) : View(context) {

    var state: Int = 0
        set(value) {
            field = value.coerceIn(0, 3)
            invalidate()
        }

    var level: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private val frames = Array(4) { mutableListOf<Bitmap>() }
    private val movies = arrayOfNulls<Movie>(4)
    private var frameIndex = 0
    private var lastFrameTime = 0L

    private val colors = intArrayOf(
        Color.rgb(0, 229, 255),
        Color.rgb(255, 230, 0),
        Color.rgb(255, 46, 99),
        Color.rgb(0, 229, 255)
    )

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        reload()
    }

    fun reload() {
        for (i in 0..3) {
            frames[i].clear()
            movies[i] = null

            val folder = File(context.filesDir, "sprites/$i")
            if (!folder.exists() || !folder.isDirectory) continue

            val files = folder.listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.name }
                ?: continue

            if (files.size == 1 &&
                files[0].extension.equals("gif", ignoreCase = true)
            ) {
                movies[i] = Movie.decodeFile(files[0].absolutePath)
            } else {
                for (file in files) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) frames[i].add(scaleBitmap(bitmap))
                }
            }
        }
        invalidate()
    }

    private fun scaleBitmap(source: Bitmap): Bitmap {
        val maxSide = 600f
        val largest = maxOf(source.width, source.height).toFloat()
        if (largest <= maxSide) return source

        val scale = maxSide / largest
        val matrix = Matrix().apply {
            setScale(scale, scale)
        }
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    fun has(targetState: Int): Boolean {
        return movies.getOrNull(targetState) != null ||
            frames.getOrNull(targetState)?.isNotEmpty() == true
    }

    private fun drawSprite(canvas: Canvas, targetState: Int): Boolean {
        val movie = movies[targetState]
        if (movie != null) {
            val duration = movie.duration().coerceAtLeast(1)
            movie.setTime((System.currentTimeMillis() % duration).toInt())

            val scale = min(
                width.toFloat() / movie.width().coerceAtLeast(1),
                height.toFloat() / movie.height().coerceAtLeast(1)
            )
            val w = movie.width() * scale
            val h = movie.height() * scale
            canvas.save()
            canvas.translate((width - w) / 2f, (height - h) / 2f)
            canvas.scale(scale, scale)
            movie.draw(canvas, 0f, 0f)
            canvas.restore()
            return true
        }

        val list = frames[targetState]
        if (list.isEmpty()) return false

        val now = System.currentTimeMillis()
        if (now - lastFrameTime > 90L) {
            frameIndex = (frameIndex + 1) % list.size
            lastFrameTime = now
        }

        val bitmap = list[frameIndex]
        val scale = min(
            width.toFloat() / bitmap.width,
            height.toFloat() / bitmap.height
        )
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val left = (width - w) / 2f
        val top = (height - h) / 2f

        paint.alpha = 255
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + w, top + h),
            paint
        )
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        if (drawSprite(canvas, state)) {
            postInvalidateDelayed(33)
            return
        }

        val cx = width / 2f
        val cy = height / 2f
        val radius = size * 0.29f
        val pulse = 1f + sin(System.currentTimeMillis() / 420.0).toFloat() * 0.035f
        val color = colors[state]

        paint.shader = RadialGradient(
            cx, cy, radius * 2.1f,
            intArrayOf(
                (color and 0x00FFFFFF) or 0x66000000,
                (color and 0x00FFFFFF) or 0x22000000,
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * 2.1f, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.012f
        paint.color = color
        paint.alpha = 100
        canvas.drawCircle(cx, cy, radius * 1.42f * pulse, paint)

        paint.strokeWidth = size * 0.008f
        paint.alpha = 210
        bounds.set(
            cx - radius * 1.65f,
            cy - radius * 1.65f,
            cx + radius * 1.65f,
            cy + radius * 1.65f
        )
        val rotation = (System.currentTimeMillis() / 18L % 360L).toFloat()
        canvas.drawArc(bounds, rotation, 95f, false, paint)
        canvas.drawArc(bounds, rotation + 180f, 45f, false, paint)

        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.shader = RadialGradient(
            cx - radius * 0.28f,
            cy - radius * 0.32f,
            radius * 1.5f,
            intArrayOf(Color.WHITE, color, Color.rgb(3, 15, 30)),
            floatArrayOf(0f, 0.28f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * pulse, paint)
        paint.shader = null

        paint.color = Color.WHITE
        paint.alpha = 190
        canvas.drawCircle(
            cx - radius * 0.28f,
            cy - radius * 0.32f,
            radius * 0.12f,
            paint
        )

        if (state == 2 || state == 3) {
            paint.color = color
            paint.alpha = 190
            paint.strokeWidth = size * 0.018f
            paint.style = Paint.Style.STROKE
            val meterRadius = radius * 1.85f
            bounds.set(
                cx - meterRadius,
                cy - meterRadius,
                cx + meterRadius,
                cy + meterRadius
            )
            canvas.drawArc(
                bounds,
                -90f,
                360f * level.coerceAtLeast(0.12f),
                false,
                paint
            )
            paint.style = Paint.Style.FILL
        }

        paint.alpha = 255
        postInvalidateDelayed(33)
    }
}
