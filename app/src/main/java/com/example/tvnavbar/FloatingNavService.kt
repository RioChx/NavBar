package com.example.tvnavbar

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.text.*
import android.text.style.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
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
        initForeground()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_nav, null)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 500
        params.y = 800

        setupUI()
        setupDrag(floatingView, params)
        startClock()
        
        MainOverride.setUpdateListener { applyVisuals() }
        windowManager.addView(floatingView, params)
    }

    private fun initForeground() {
        val channelId = "tv_nav"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "TV Nav", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details).build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, notification)
        }
    }

    private fun applyVisuals() {
        val root = floatingView.findViewById<View>(R.id.nav_root)
        root.background.setTint(MainOverride.backgroundColor)
        root.alpha = MainOverride.transparency / 100f
        root.scaleX = MainOverride.scale
        root.scaleY = MainOverride.scale
    }

    private fun setupUI() {
        applyVisuals()
        
        val btnClose = floatingView.findViewById<View>(R.id.btn_close)
        val btnSettings = floatingView.findViewById<View>(R.id.btn_settings)
        
        // Hover Logic
        floatingView.setOnHoverListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    btnClose.animate().alpha(1f).setDuration(200).start()
                    btnSettings.animate().alpha(1f).setDuration(200).start()
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    btnClose.animate().alpha(0f).setDuration(200).start()
                    btnSettings.animate().alpha(0f).setDuration(200).start()
                }
            }
            false
        }

        floatingView.findViewById<View>(R.id.btn_home).setOnClickListener {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        btnSettings.setOnClickListener {
            startService(Intent(this, ControlOverlayService::class.java))
        }

        btnClose.setOnClickListener {
            stopService(Intent(this, ControlOverlayService::class.java))
            stopSelf()
        }

        // Double Click to Minimize
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isMinimized = !isMinimized
                floatingView.findViewById<View>(R.id.nav_buttons_container).visibility = if (isMinimized) View.GONE else View.VISIBLE
                floatingView.findViewById<View>(R.id.tv_date).visibility = if (isMinimized) View.GONE else View.VISIBLE
                return true
            }
        })
        floatingView.findViewById<View>(R.id.clock_container).setOnTouchListener { v, event ->
            detector.onTouchEvent(event)
            v.performClick()
            true
        }
    }

    private fun setupDrag(view: View, p: WindowManager.LayoutParams) {
        var startX = 0; var startY = 0; var startTouchX = 0f; var startTouchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y
                    startTouchX = event.rawX; startTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (event.rawX - startTouchX).toInt()
                    p.y = startY + (event.rawY - startTouchY).toInt()
                    windowManager.updateViewLayout(view, p)
                    true
                }
                else -> false
            }
        }
    }

    private fun startClock() {
        val tvTime = floatingView.findViewById<TextView>(R.id.tv_time)
        val tvDate = floatingView.findViewById<TextView>(R.id.tv_date)
        val tick = object : Runnable {
            override fun run() {
                val cal = Calendar.getInstance()
                val colon = if (blinkState) ":" else " "
                val timeStr = SimpleDateFormat("hh" + "'" + colon + "'" + "mm", Locale.US).format(cal.time)
                val ampm = SimpleDateFormat(" a", Locale.US).format(cal.time)
                
                val builder = SpannableStringBuilder(timeStr)
                builder.setSpan(ForegroundColorSpan(MainOverride.colorTimeNumeric), 0, builder.length, 0)
                
                val startAmPm = builder.length
                builder.append(ampm)
                builder.setSpan(RelativeSizeSpan(0.5f), startAmPm, builder.length, 0)
                builder.setSpan(ForegroundColorSpan(MainOverride.colorAmPm), startAmPm, builder.length, 0)
                
                tvTime.text = builder
                tvTime.typeface = MainOverride.getTypeface()
                
                tvDate.text = SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(cal.time)
                tvDate.setTextColor(MainOverride.colorDate)
                tvDate.typeface = MainOverride.getTypeface()
                
                blinkState = !blinkState
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tick)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
