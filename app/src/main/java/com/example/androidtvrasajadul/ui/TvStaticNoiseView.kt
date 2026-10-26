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
 * Lightweight CRT TV static noise ("gambar semut TV dinamis") optimized for Android TV sticks.
 * Uses 4 pre-generated noise bitmaps to eliminate runtime CPU/memory churn on the UI thread.
 */
class TvStaticNoiseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val noiseWidth = 120
    private val noiseHeight = 68
    private val frameCount = 4
    private val frames = Array(frameCount) { Bitmap.createBitmap(noiseWidth, noiseHeight, Bitmap.Config.ARGB_8888) }
    private val paint = Paint().apply { isFilterBitmap = false }
    private val srcRect = Rect(0, 0, noiseWidth, noiseHeight)
    private val destRect = Rect()

    private var currentFrame = 0
    private var isRunning = false

    init {
        val random = Random()
        val pixels = IntArray(noiseWidth * noiseHeight)
        // Pre-bake 4 random noise frames at startup
        for (f in 0 until frameCount) {
            for (i in pixels.indices) {
                val v = random.nextInt(256)
                pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
            frames[f].setPixels(pixels, 0, noiseWidth, 0, 0, noiseWidth, noiseHeight)
        }
    }

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

        // Fast draw from pre-generated frame without any memory allocation
        val bitmap = frames[currentFrame % frameCount]
        canvas.drawBitmap(bitmap, srcRect, destRect, paint)
        currentFrame++

        if (isRunning) {
            postInvalidateDelayed(50) // ~20 FPS static noise — light on TV stick CPU
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopNoise()
    }
}
