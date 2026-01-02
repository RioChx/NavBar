package com.example.tvnavbar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class FloatingNavService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    
    private var isMinimized = false
    private val handler = Handler(Looper.getMainLooper())
    private var blinkState = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_nav, null)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        setupUI()
        setupDrag()
        startClock()
        
        windowManager.addView(floatingView, params)
    }

    private fun setupUI() {
        val root = floatingView.findViewById<LinearLayout>(R.id.nav_root)
        root.setBackgroundColor(MainOverride.backgroundColor)
        root.alpha = MainOverride.transparency / 100f
        root.scaleX = MainOverride.scale
        root.scaleY = MainOverride.scale

        val btnSettings = floatingView.findViewById<ImageView>(R.id.btn_settings)
        val btnClose = floatingView.findViewById<ImageView>(R.id.btn_close)
        
        btnSettings.visibility = View.GONE
        btnClose.visibility = View.GONE

        floatingView.setOnHoverListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    btnSettings.visibility = View.VISIBLE
                    btnClose.visibility = View.VISIBLE
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    btnSettings.visibility = View.GONE
                    btnClose.visibility = View.GONE
                }
            }
            false
        }

        btnSettings.setOnClickListener {
            startService(Intent(this, ControlOverlayService::class.java))
        }

        btnClose.setOnClickListener {
            stopSelf()
            stopService(Intent(this, ControlOverlayService::class.java))
        }

        val clockContainer = floatingView.findViewById<View>(R.id.clock_container)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleMinimize()
                return true
            }
        })

        clockContainer.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            false
        }
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        val navButtons = floatingView.findViewById<View>(R.id.nav_buttons)
        val tvDate = floatingView.findViewById<View>(R.id.tv_date)
        
        if (isMinimized) {
            navButtons.visibility = View.GONE
            tvDate.visibility = View.GONE
        } else {
            navButtons.visibility = View.VISIBLE
            tvDate.visibility = View.VISIBLE
        }
    }

    private fun startClock() {
        val tvTime = floatingView.findViewById<TextView>(R.id.tv_time)
        val tvDate = floatingView.findViewById<TextView>(R.id.tv_date)
        
        val runnable = object : Runnable {
            override fun run() {
                val calendar = Calendar.getInstance()
                val sdfDate = SimpleDateFormat("EEE dd MMM yyyy", Locale.US)
                tvDate.text = sdfDate.format(calendar.time)

                val colon = if (blinkState) ":" else " "
                val timeStr = SimpleDateFormat("hh" + "'" + colon + "'" + "mm a", Locale.US).format(calendar.time)
                
                val spannable = SpannableString(timeStr)
                if (timeStr.length > 3) {
                    spannable.setSpan(RelativeSizeSpan(0.6f), timeStr.length - 2, timeStr.length, 0)
                }
                
                tvTime.text = spannable
                blinkState = !blinkState
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun setupDrag() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView.setOnTouchListener { v, event ->
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
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        handler.removeCallbacksAndMessages(null)
    }
}
