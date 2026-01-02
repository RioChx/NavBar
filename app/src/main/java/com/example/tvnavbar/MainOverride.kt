package com.example.tvnavbar

import android.graphics.Color
import android.graphics.Typeface

/**
 * MANDATORY: Single source of truth for UI settings.
 */
object MainOverride {
    var backgroundColor = 0xFF1A1A1A
    var transparency = 85
    var scale = 1f
    var textColorTime = 0xFFFFFFFF
    var textColorDate = 0xFFB0B0B0
    var fontType = "Roboto"

    fun getTypeface(): Typeface {
        return try {
            Typeface.create(fontType, Typeface.NORMAL)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
}
