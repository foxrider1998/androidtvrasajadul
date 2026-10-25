package com.example.androidtvrasajadul.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import java.util.Random

/**
 * Custom View that generates animated CRT TV static noise ("gambar semut TV dinamis")
 * for a realistic nostalgic TV experience during "TIDAK ADA SINYAL" state.
 */
class TvStaticNoiseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val noiseWidth = 160
    private val noiseHeight = 90
    private val pixels = IntArray(noiseWidth * noiseHeight)
    private val noiseBitmap = Bitmap.createBitmap(noiseWidth, noiseHeight, Bitmap.Config.ARGB_8888)
    private val paint = Paint().apply { isFilterBitmap = false }
    private val random = Random()
    private val srcRect = Rect(0, 0, noiseWidth, noiseHeight)
    private val destRect = Rect()

    private var isRunning = false

    fun startNoise() {
        if (!isRunning) {
            isRunning = true
            postInvalidate()
        }
    }

    fun stopNoise() {
        isRunning = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        destRect.set(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isRunning) return

        // Generate fast random black/white/gray pixel noise
        for (i in pixels.indices) {
            val v = random.nextInt(256)
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

        noiseBitmap.setPixels(pixels, 0, noiseWidth, 0, 0, noiseWidth, noiseHeight)
        canvas.drawBitmap(noiseBitmap, srcRect, destRect, paint)

        if (isRunning) {
            postInvalidateDelayed(33) // ~30 FPS TV static noise
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopNoise()
    }
}
