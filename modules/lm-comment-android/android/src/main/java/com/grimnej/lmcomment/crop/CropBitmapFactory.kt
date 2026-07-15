package com.grimnej.lmcomment.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/** Creates an independent in-memory bitmap; it never writes screenshot data to disk. */
object CropBitmapFactory {
    fun create(source: Bitmap, selection: NormalizedCropRect): Bitmap {
        check(!source.isRecycled) { "Cannot crop a recycled frame." }
        val crop = CropGeometry.toPixels(selection, source.width, source.height)
        val outputConfig = source.config
            ?.takeUnless { it == Bitmap.Config.HARDWARE }
            ?: Bitmap.Config.ARGB_8888
        val output = Bitmap.createBitmap(crop.width, crop.height, outputConfig)
        var complete = false
        try {
            output.density = source.density
            output.setHasAlpha(source.hasAlpha())
            Canvas(output).drawBitmap(
                source,
                Rect(crop.left, crop.top, crop.right, crop.bottom),
                Rect(0, 0, crop.width, crop.height),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            complete = true
            return output
        } finally {
            if (!complete && !output.isRecycled) output.recycle()
        }
    }
}
