package com.example.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object SampleWasteBitmapGenerator {

    /**
     * Creates a high quality illustrative Bitmap representing a sample waste item
     * so that users on emulators or without photos can test the AI camera analysis.
     */
    fun createSampleBitmap(itemName: String, mainColor: Int = Color.rgb(56, 142, 60)): Bitmap {
        val width = 480
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Container
        val cardPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(30f, 30f, (width - 30).toFloat(), (height - 30).toFloat())
        canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)

        // Inner shape
        val shapePaint = Paint().apply {
            color = mainColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val innerRect = RectF(60f, 100f, (width - 60).toFloat(), (height - 120).toFloat())
        canvas.drawRoundRect(innerRect, 20f, 20f, shapePaint)

        // Label header text
        val titlePaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 34f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(itemName, (width / 2).toFloat(), 75f, titlePaint)

        // Inner text (AI Sample Photo badge)
        val innerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("AI 識別テスト画像", (width / 2).toFloat(), (height / 2).toFloat() - 10f, innerTextPaint)
        
        val subTextPaint = Paint().apply {
            color = Color.rgb(220, 240, 225)
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("【$itemName】", (width / 2).toFloat(), (height / 2).toFloat() + 35f, subTextPaint)

        // Footer note
        val footerPaint = Paint().apply {
            color = Color.rgb(120, 120, 120)
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("撮影サンプル（エミュレータ・検証用）", (width / 2).toFloat(), (height - 60).toFloat(), footerPaint)

        return bitmap
    }
}
