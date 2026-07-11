package com.example.notifspike

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotifSpike", "Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val timestamp = sbn.postTime

        Log.d("NotifSpike", "App: $packageName | Title: $title | Text: $text")

        // Save to in-memory store so MainActivity can display it
        val entry = CapturedNotification(
            packageName = packageName,
            title = title,
            text = text,
            timestamp = timestamp
        )
        NotificationStore.add(entry)

        // Broadcast so MainActivity can refresh live if it's open
        val intent = Intent(ACTION_NEW_NOTIFICATION)
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for the spike
    }

    companion object {
        const val ACTION_NEW_NOTIFICATION = "com.example.notifspike.NEW_NOTIFICATION"
    }
}

data class CapturedNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

// Simple in-memory store — good enough for the spike.
// Not persisted across app restarts; that's fine, this is just to prove capture works.
object NotificationStore {
    private val items = mutableListOf<CapturedNotification>()

    fun add(item: CapturedNotification) {
        items.add(0, item) // newest first
    }

    fun getAll(): List<CapturedNotification> = items.toList()

    fun clear() = items.clear()
}
