package com.example.focusreminder.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.focusreminder.R
import com.example.focusreminder.platform.UsagePermissionHelper
import java.util.Locale
import kotlin.math.abs

/**
 * Floating status bar capsule overlay that renders a live ticking countdown pill
 * identical to OEM status bar timer chips (OxygenOS / ColorOS Fluid Cloud, HyperOS).
 *
 * Positions directly in the status bar area next to the clock and is smoothly draggable.
 */
class FloatingTimerOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pillView: LinearLayout? = null
    private var timerTextView: TextView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var targetEndTimeMs: Long = 0L
    private var activeAppName: String = "App"
    private var isAttached = false

    // Drag tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // Tick runnable
    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (!isAttached || pillView == null) return

            val now = System.currentTimeMillis()
            val remainingMs = maxOf(0L, targetEndTimeMs - now)
            val totalSeconds = remainingMs / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L

            timerTextView?.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)

            if (remainingMs > 0L) {
                // Schedule next tick precisely on second boundary
                val nextDelay = maxOf(100L, 1000L - (now % 1000L))
                mainHandler.postDelayed(this, nextDelay)
            } else {
                dismiss()
            }
        }
    }

    /**
     * Shows the floating countdown capsule at the top status bar.
     */
    fun show(appName: String, endTimeMs: Long) {
        if (!UsagePermissionHelper.hasOverlayPermission(context)) {
            return
        }

        activeAppName = appName
        targetEndTimeMs = endTimeMs

        mainHandler.post {
            if (isAttached && pillView != null) {
                // Already attached: just update target and trigger ticker
                mainHandler.removeCallbacks(tickerRunnable)
                mainHandler.post(tickerRunnable)
                return@post
            }

            createAndAttachPill()
        }
    }

    /**
     * Dismisses and removes the floating capsule from WindowManager.
     */
    fun dismiss() {
        mainHandler.post {
            mainHandler.removeCallbacks(tickerRunnable)
            if (isAttached && pillView != null && windowManager != null) {
                try {
                    windowManager.removeView(pillView)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isAttached = false
                    pillView = null
                    timerTextView = null
                }
            }
        }
    }

    val isShowing: Boolean
        get() = isAttached && pillView != null

    private fun createAndAttachPill() {
        if (windowManager == null) return

        // 1. Calculate status bar dimensions
        val statusBarHeight = getStatusBarHeight()
        val pillHeightPx = dpToPx(24)
        val startY = maxOf(dpToPx(4), (statusBarHeight - pillHeightPx) / 2)
        val startX = dpToPx(72) // Docks just right of standard 4-digit 12/24hr status bar clock

        // 2. Build WindowManager LayoutParams
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }
        windowParams = params

        // 3. Create Root Capsule View (Vibrant Purple / Indigo Pill matching OxygenOS Fluid Cloud)
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(3), dpToPx(10), dpToPx(3))

            // Capsule rounded drawable
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#4F46E5")) // Vibrant Indigo/Purple (RGB 79, 70, 229)
                setStroke(dpToPx(1), Color.parseColor("#818CF8")) // Subtle highlight border
            }
            background = bgDrawable
            elevation = dpToPx(6).toFloat()
        }

        // 4. Stopwatch Vector Icon
        val icon = ImageView(context).apply {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_stat_timer)
            setImageDrawable(drawable)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                marginEnd = dpToPx(5)
            }
        }
        pill.addView(icon)

        // 5. Monospaced Digital Countdown TextView
        val textView = TextView(context).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 12f
            setTextColor(Color.WHITE)
            includeFontPadding = false
            text = "00:00"
        }
        timerTextView = textView
        pill.addView(textView)

        // 6. Interactive Drag & Touch Handling
        pill.setOnTouchListener { view, event ->
            val p = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > dpToPx(4) || abs(dy) > dpToPx(4)) {
                        isDragging = true
                        p.x = initialX + dx
                        p.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(pill, p)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // User tapped the capsule
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    true
                }
                else -> false
            }
        }

        pillView = pill

        try {
            windowManager.addView(pill, params)
            isAttached = true
            mainHandler.post(tickerRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
            isAttached = false
            pillView = null
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            dpToPx(32)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
