package com.example.androidtvrasajadul.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.androidtvrasajadul.MainActivity

/**
 * BroadcastReceiver triggered on system boot to automatically launch the main TV tuner activity.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received boot broadcast action: $action")
        if (Intent.ACTION_BOOT_COMPLETED == action ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED == action ||
            Intent.ACTION_REBOOT == action ||
            "android.intent.action.QUICKBOOT_POWERON" == action ||
            "com.htc.intent.action.QUICKBOOT_POWERON" == action
        ) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            try {
                context.startActivity(launchIntent)
                Log.d(TAG, "Successfully started MainActivity from BootReceiver")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MainActivity from BootReceiver: ${e.message}", e)
            }
        }
    }
}
