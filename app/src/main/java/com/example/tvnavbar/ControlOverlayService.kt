package com.example.tvnavbar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.SeekBar
import android.widget.TextView

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
            600,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        
        setupControls()
        setupDrag()
        windowManager.addView(controlView, params)
    }

    private fun setupControls() {
        val seekTrans = controlView.findViewById<SeekBar>(R.id.seek_transparency)
        seekTrans.progress = MainOverride.transparency
        seekTrans.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                MainOverride.transparency = p1
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        
        controlView.findViewById<View>(R.id.btn_close_control).setOnClickListener { stopSelf() }
    }

    private fun setupDrag() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        controlView.findViewById<View>(R.id.control_header).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(controlView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::controlView.isInitialized) windowManager.removeView(controlView)
    }
}
