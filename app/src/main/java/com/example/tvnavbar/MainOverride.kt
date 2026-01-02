package com.example.tvnavbar

import android.graphics.Color
import android.graphics.Typeface

/**
 * MANDATORY: The single source of truth for all UI settings.
 */
object MainOverride {
    // Global Variables
    var backgroundColor = 0xFF1A1A1A
    var transparency = 85 // 0-100
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
