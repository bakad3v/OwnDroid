package com.bintianqi.owndroid

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bintianqi.owndroid.dpm.getDPM
import com.bintianqi.owndroid.dpm.getReceiver
import com.bintianqi.owndroid.encryption.EncryptionManager

class AutomationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        handleTask(context, intent)
    }
}

@SuppressLint("NewApi")
fun handleTask(context: Context, intent: Intent): String {
    val sharedPrefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
    val encryptionManager = EncryptionManager()
    val hash = sharedPrefs.getString("automation_key", "") ?: ""
    val salt = sharedPrefs.getString("salt","") ?: ""
    val key = intent.getStringExtra("key") ?: ""
    if(!encryptionManager.checkPassword(key,hash, salt)) {
        return "Wrong key"
    }
    val operation = intent.getStringExtra("operation")
    val dpm = context.getDPM()
    val receiver = context.getReceiver()
    val app = intent.getStringExtra("app")
    val restriction = intent.getStringExtra("restriction")
    try {
        when(operation) {
            "suspend" -> dpm.setPackagesSuspended(receiver, arrayOf(app), true)
            "unsuspend" -> dpm.setPackagesSuspended(receiver, arrayOf(app), false)
            "hide" -> dpm.setApplicationHidden(receiver, app, true)
            "unhide" -> dpm.setApplicationHidden(receiver, app, false)
            "lock" -> dpm.lockNow()
            "reboot" -> dpm.reboot(receiver)
            "addUserRestriction" -> dpm.addUserRestriction(receiver, restriction)
            "clearUserRestriction" -> dpm.clearUserRestriction(receiver, restriction)
            else -> return "Operation not defined"
        }
    } catch(e: Exception) {
        return e.message ?: "Failed to get error message"
    }
    return "No error, or error is unhandled"
}
