package com.example.tvnavbar

import android.graphics.Color
import android.graphics.Typeface

/**
 * MANDATORY: Single source of truth for UI settings.
 * Fixed: Using Color.parseColor to ensure Int type for all colors.
 */
object MainOverride {
    var backgroundColor: Int = Color.parseColor("#1a1a1a")
    var transparency: Int = 85
    var scale: Float = 1f
    
    // 3 Different Text Style Controls
    var colorTimeNumeric: Int = Color.parseColor("#ffffff")
    var colorAmPm: Int = Color.parseColor("#ffffff")
    var colorDate: Int = Color.parseColor("#b0b0b0")
    
    var fontType: String = "Roboto"

    fun getTypeface(): Typeface {
        return try {
            Typeface.create(fontType, Typeface.NORMAL)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
}
