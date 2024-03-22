package com.luoxuchan.contrastdc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // 这里演示的是显示一个Toast消息
            // 实际上，您可以在这里启动一个后台服务
            Toast.makeText(context, "已正常唤起CDC", Toast.LENGTH_SHORT).show()

            // 示例：启动后台服务
            // val serviceIntent = Intent(context, YourService::class.java)
            // context.startService(serviceIntent)
        }
    }
}
