package com.seniorenlauncher.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.seniorenlauncher.LauncherApp
import com.seniorenlauncher.MainActivity

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val address = messages.firstOrNull()?.displayOriginatingAddress ?: "Desconocido"
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        showNotification(context.applicationContext, address, fullBody)
        saveMessageToInbox(context.applicationContext, address, fullBody, timestamp)
    }

    private fun saveMessageToInbox(context: Context, address: String, body: String, date: Long) {
        val v = ContentValues().apply { put(Telephony.Sms.ADDRESS, address); put(Telephony.Sms.BODY, body); put(Telephony.Sms.DATE, date); put(Telephony.Sms.READ, 0); put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX) }
        try { context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, v) } catch (e: Exception) {}
    }

    private fun showNotification(context: Context, address: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP; putExtra("NAVIGATE_TO", "sms"); putExtra("SMS_ADDRESS", address) }
        val pi = PendingIntent.getActivity(context, address.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, LauncherApp.CH_SMS).setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle(address).setContentText(body).setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(true).setContentIntent(pi)
        nm.notify(address.hashCode(), builder.build())
    }
}
