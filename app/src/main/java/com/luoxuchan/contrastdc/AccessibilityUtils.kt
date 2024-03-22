package com.luoxuchan.contrastdc

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "AccessibilityServiceChannel"
        val descriptionText = "用于提示无障碍服务状态的通知渠道"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("ACCESSIBILITY_SERVICE_CHANNEL_ID", name, importance).apply {
            description = descriptionText
        }
        // 注册通知渠道
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun showAccessibilityServiceDisabledNotification(context: Context) {
    // 创建一个Intent，用于启动主界面MainActivity
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    // 根据Android版本构建Notification.Builder
    val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, "ACCESSIBILITY_SERVICE_CHANNEL_ID")
    } else {
        Notification.Builder(context)
    }

    // 构建通知，并设置点击行为
    val notification: Notification = notificationBuilder
        .setContentTitle("应用功能不可用")
        .setContentText("点击以启用无障碍服务")
        .setSmallIcon(R.drawable.ic_launcher_foreground) // 设置一个小图标
        .setContentIntent(pendingIntent) // 设置点击通知后的操作
        .setOngoing(true) // 使通知常驻
        .build()

    // 获取NotificationManager并发布通知
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(1, notification) // 1 是通知的 ID
}

fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
    val expectedComponentName = ComponentName(context, service)
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServicesSetting)
    while (splitter.hasNext()) {
        val componentNameString = splitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName)
            return true
    }
    return false
}

fun getSystemBrightnessFlow(context: Context) = callbackFlow {
    val contentResolver = context.contentResolver

    // 查询并发送当前亮度值的函数
    fun queryAndSendCurrentBrightness() {
        val currentBrightness = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            127 // Default brightness in case of exception
        }
        trySend(currentBrightness).isSuccess
    }

    val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            queryAndSendCurrentBrightness()
        }
    }

    contentResolver.registerContentObserver(
        Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
        true,
        brightnessObserver
    )

    // 立即查询并发送当前屏幕亮度值
    queryAndSendCurrentBrightness()

    // 当Flow被取消订阅时，注销内容观察者
    awaitClose { contentResolver.unregisterContentObserver(brightnessObserver) }
}


fun calculateOverlayAlpha(brightness: Int): Int {
    // 基于提供的表格，创建一个遮罩透明度数组
    val alphaValues = arrayOf(
        100,83,70,60,53,45,40,32,25,20,18,17,16,15,13,12,11,10,9,9,8,8,7,
        6,5,5,5,4,4,4,3,3,3,3,3,3,3,3,3,2,2,2,2,2,2,2,2,2,2,1,1,1,1,1,1,1
    ) + IntArray(256 - 56) { 0 }.toList()

    // 使用亮度值作为索引来获取遮罩透明度
    return alphaValues[brightness.coerceIn(0, 255)]
}
class AccessibilityUtils {

}