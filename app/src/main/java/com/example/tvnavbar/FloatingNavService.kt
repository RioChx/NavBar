package com.example.tvnavbar

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Typeface
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
        // MUST be first for Android 13
        startForegroundSafe()
        
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
        params.x = 400
        params.y = 800

        setupUI()
        setupDrag()
        startClock()
        
        MainOverride.setUpdateListener { applyVisuals() }
        
        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun startForegroundSafe() {
        val channelId = "tv_nav_v2"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "TV Nav Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Floating Navigation Active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(2002, notification)
    }

    private fun applyVisuals() {
        val root = floatingView.findViewById<View>(R.id.nav_root)
        root.background?.setTint(MainOverride.backgroundColor)
        root.alpha = MainOverride.transparency / 100f
        root.scaleX = MainOverride.scale
        root.scaleY = MainOverride.scale
    }

    private fun setupUI() {
        applyVisuals()
        val btnClose = floatingView.findViewById<View>(R.id.btn_close)
        val btnSettings = floatingView.findViewById<View>(R.id.btn_settings)
        
        btnClose.alpha = 0f
        btnSettings.alpha = 0f

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
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            startService(Intent(this, ControlOverlayService::class.java))
        }

        btnClose.setOnClickListener {
            stopService(Intent(this, ControlOverlayService::class.java))
            stopSelf()
        }

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isMinimized = !isMinimized
                floatingView.findViewById<View>(R.id.nav_buttons_container).visibility = if (isMinimized) View.GONE else View.VISIBLE
                floatingView.findViewById<View>(R.id.divider).visibility = if (isMinimized) View.GONE else View.VISIBLE
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

    private fun setupDrag() {
        var initialX = 0; var initialY = 0; var touchX = 0f; var touchY = 0f
        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
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
        val tick = object : Runnable {
            override fun run() {
                val cal = Calendar.getInstance()
                val colon = if (blinkState) ":" else " "
                val timeStr = SimpleDateFormat("hh" + "'" + colon + "'" + "mm", Locale.US).format(cal.time)
                val ampm = SimpleDateFormat(" a", Locale.US).format(cal.time)
                
                val ss = SpannableStringBuilder(timeStr)
                ss.setSpan(ForegroundColorSpan(MainOverride.colorTimeNumeric), 0, ss.length, 0)
                ss.setSpan(StyleSpan(Typeface.BOLD), 0, ss.length, 0)
                
                val startAmPm = ss.length
                ss.append(ampm)
                ss.setSpan(RelativeSizeSpan(0.5f), startAmPm, ss.length, 0)
                ss.setSpan(ForegroundColorSpan(MainOverride.colorAmPm), startAmPm, ss.length, 0)
                
                tvTime.text = ss
                tvTime.typeface = MainOverride.getTypeface()
                tvDate.text = SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(cal.time)
                tvDate.setTextColor(MainOverride.colorDate)
                
                blinkState = !blinkState
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tick)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) try { windowManager.removeView(floatingView) } catch(e: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }
}
