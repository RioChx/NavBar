package com.example.tvnavbar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
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
    private val handler = Handler(Looper.getMainLooper())
    private var blinkState = true
    private var isMinimized = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_nav, null)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 50

        setupUI()
        setupDrag()
        startClock()
        windowManager.addView(floatingView, params)
    }

    private fun setupUI() {
        val root = floatingView.findViewById<LinearLayout>(R.id.nav_root)
        root.setBackgroundResource(R.drawable.bg_nav_pill)
        
        // Fixed: MainOverride.backgroundColor is now Int
        root.background?.setTint(MainOverride.backgroundColor)
        root.alpha = MainOverride.transparency / 100f
        root.scaleX = MainOverride.scale
        root.scaleY = MainOverride.scale
        
        val btnClose = floatingView.findViewById<ImageView>(R.id.btn_close)
        val btnSettings = floatingView.findViewById<ImageView>(R.id.btn_settings)
        btnClose.visibility = View.GONE
        btnSettings.visibility = View.GONE

        floatingView.setOnHoverListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    btnClose.visibility = View.VISIBLE
                    btnSettings.visibility = View.VISIBLE
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    btnClose.visibility = View.GONE
                    btnSettings.visibility = View.GONE
                }
            }
            false
        }

        floatingView.findViewById<ImageView>(R.id.btn_home).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            startService(Intent(this, ControlOverlayService::class.java))
        }

        btnClose.setOnClickListener {
            stopService(Intent(this, ControlOverlayService::class.java))
            stopSelf()
        }

        val clockContainer = floatingView.findViewById<View>(R.id.clock_container)
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isMinimized = !isMinimized
                floatingView.findViewById<View>(R.id.nav_buttons).visibility = if (isMinimized) View.GONE else View.VISIBLE
                floatingView.findViewById<View>(R.id.tv_date).visibility = if (isMinimized) View.GONE else View.VISIBLE
                return true
            }
        })
        clockContainer.setOnTouchListener { v, event ->
            detector.onTouchEvent(event)
            v.performClick()
            true
        }
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

    private fun startClock() {
        val tvTime = floatingView.findViewById<TextView>(R.id.tv_time)
        val tvDate = floatingView.findViewById<TextView>(R.id.tv_date)
        val runnable = object : Runnable {
            override fun run() {
                val cal = Calendar.getInstance()
                val colon = if (blinkState) ":" else " "
                
                // Parts: Numeric Time, AM/PM, Date
                val timeNumeric = SimpleDateFormat("hh" + "'" + colon + "'" + "mm", Locale.US).format(cal.time)
                val ampm = SimpleDateFormat(" a", Locale.US).format(cal.time)
                val dateStr = SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(cal.time)
                
                // Build styled time string
                val builder = SpannableStringBuilder()
                builder.append(timeNumeric)
                
                // Fixed: ForegroundColorSpan now receives Int
                builder.setSpan(ForegroundColorSpan(MainOverride.colorTimeNumeric), 0, builder.length, 0)
                
                val ampmStart = builder.length
                builder.append(ampm)
                builder.setSpan(RelativeSizeSpan(0.6f), ampmStart, builder.length, 0)
                builder.setSpan(ForegroundColorSpan(MainOverride.colorAmPm), ampmStart, builder.length, 0)
                
                tvTime.text = builder
                tvTime.typeface = MainOverride.getTypeface()
                
                // Date styling
                tvDate.text = dateStr
                tvDate.setTextColor(MainOverride.colorDate)
                tvDate.typeface = MainOverride.getTypeface()
                
                blinkState = !blinkState
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        handler.removeCallbacksAndMessages(null)
    }
}
