package com.example.tvnavbar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.SeekBar

class ControlOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var controlView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        controlView = LayoutInflater.from(this).inflate(R.layout.layout_control_overlay, null)
        
        params = WindowManager.LayoutParams(
            800, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        
        setupControls()
        windowManager.addView(controlView, params)
    }

    private fun setupControls() {
        val sTrans = controlView.findViewById<SeekBar>(R.id.seek_transparency)
        sTrans.progress = MainOverride.transparency
        sTrans.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                MainOverride.transparency = p1
                MainOverride.notifyUpdate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        controlView.findViewById<View>(R.id.btn_close_control).setOnClickListener { stopSelf() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::controlView.isInitialized) try { windowManager.removeView(controlView) } catch(e: Exception) {}
    }
}
