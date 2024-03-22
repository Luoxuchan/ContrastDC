package com.luoxuchan.contrastdc

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.luoxuchan.contrastdc.ui.theme.ContrastDCTheme
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + this.packageName)
            startActivity(intent)
        }
        setContent {
            ContrastDCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
        createNotificationChannel(this)
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    var isServiceEnabled by remember { mutableStateOf(false) }
    var isOverlayEnabled by remember { mutableStateOf(true) }
    var autoAdjustAlpha by remember { mutableStateOf(true) }
    var manualAlpha by remember { mutableFloatStateOf(64f) }
    val brightness by getSystemBrightnessFlow(context).collectAsState(initial = 127)

    // 监听生命周期事件，每次界面回到前台时更新无障碍服务状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context, ContrastAccessibilityService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var overlayAlpha = if (autoAdjustAlpha) calculateOverlayAlpha(brightness) else manualAlpha.toInt()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isServiceEnabled) "无障碍服务已启用" else "无障碍服务未启用",
            color = if (isServiceEnabled) Color.Green else Color.Red
        )

        // 当无障碍服务未启用时显示“打开无障碍设置”按钮
        if (!isServiceEnabled) {
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) {
                showAccessibilityServiceDisabledNotification(context)
                Text("打开无障碍设置")
            }
        }

        // 当无障碍服务已启用时，显示“遮罩开关”及其开关
        if (isServiceEnabled) {
            // 获取Context实例，适用于Composable函数。对于非Composable函数，使用相应传入的Context实例
            val context: Context = LocalContext.current
            // 获取NotificationManager实例
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 取消ID为1的通知
            notificationManager.cancel(1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("遮罩开关")
                Switch(
                    checked = isOverlayEnabled,
                    onCheckedChange = { isEnabled ->
                        isOverlayEnabled = isEnabled
                        updateOverlay(isOverlayEnabled, overlayAlpha, context)
                    }
                )
            }
        }
        // 显示当前系统亮度
        Text("系统亮度: $brightness")
        // 开关控制自动调整透明度或手动调整
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("自动调整遮罩透明度")
            Switch(
                checked = autoAdjustAlpha,
                onCheckedChange = { autoAdjustAlpha = it }
            )
        }
        if (autoAdjustAlpha) {
            LaunchedEffect(brightness, manualAlpha, isOverlayEnabled) {
                updateOverlay(isOverlayEnabled, overlayAlpha, context)
            }
            Text("自动遮罩透明度: $overlayAlpha")
        }else {
            Slider(
                value = manualAlpha,
                onValueChange = { newValue ->
                    manualAlpha = newValue
                    overlayAlpha = manualAlpha.toInt()
                    updateOverlay(isOverlayEnabled, overlayAlpha, context)
                },
                valueRange = 0f..100f,
                steps = 100
            )
            Text("手动遮罩透明度: ${manualAlpha.toInt()}")
        }
        Spacer(modifier = Modifier.height(16.dp)) // Add some space before the image
        Image(
            painter = painterResource(id = R.drawable.testpic),
            contentDescription = "Test Picture",
            modifier = Modifier
                .fillMaxWidth(1f) // 设置宽度为屏幕宽度
                .aspectRatio(1f, matchHeightConstraintsFirst = true) // 保持图片的原始宽高比，这里的1f是一个示例值，您可能需要根据实际图片的宽高比进行调整
        )
    }
}

fun updateOverlay(isEnabled: Boolean, alpha: Int, context: Context) {
    val intent = Intent("com.luoxuchan.contrastdc.ACTION_CHANGE_OVERLAY").apply {
        putExtra("isOverlayEnabled", isEnabled)
        putExtra("overlayAlpha", alpha)
    }
    context.sendBroadcast(intent)
}
