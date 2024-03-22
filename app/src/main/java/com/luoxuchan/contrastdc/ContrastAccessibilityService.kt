package com.luoxuchan.contrastdc

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.accessibility.AccessibilityEvent
import android.graphics.Color

class ContrastAccessibilityService : AccessibilityService() {

    private var overlayView: FrameLayout? = null
    private lateinit var windowManager: WindowManager

    // 创建广播接收器
    private val overlayControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isOverlayEnabled = intent?.getBooleanExtra("isOverlayEnabled", false) ?: return
            val overlayAlpha = intent?.getIntExtra("overlayAlpha", 64) ?: 64 // 默认透明度为64
            if (isOverlayEnabled) {
                // 根据传入的透明度参数显示覆盖层
                updateOverlayAlpha(overlayAlpha)
                showOverlay()
            } else {
                // 隐藏覆盖层
                removeOverlay()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()

        // 注册广播接收器
        val filter = IntentFilter("com.luoxuchan.contrastdc.ACTION_CHANGE_OVERLAY")
        registerReceiver(overlayControlReceiver, filter)
    }

    private fun createOverlay() {
        overlayView = FrameLayout(this).apply {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }
        // 默认不添加到 WindowManager，避免启动服务即显示覆盖层
    }

    private fun updateOverlayAlpha(alpha: Int) {
        overlayView?.setBackgroundColor(Color.argb(alpha, 255, 255, 255)) // 根据传入的透明度更新颜色
    }

    private fun showOverlay() {
        overlayView?.let { view ->
            if (view.parent == null) {
                windowManager.addView(view, view.layoutParams)
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            if (view.parent != null) {
                windowManager.removeView(view)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理无障碍事件时，可以留空
    }

    override fun onInterrupt() {
        // 服务中断时需要处理的逻辑，如果没有，可以留空
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        // 取消注册广播接收器
        unregisterReceiver(overlayControlReceiver)
    }
}