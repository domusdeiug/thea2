package com.example.notifspike

import android.app.Notification
import android.content.Intent
import android.os.Build
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
        val notification = sbn.notification
        val extras = notification.extras

        val flatTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val flatText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Is this a group summary notification? (e.g. WhatsApp's "New messages from 2 chats")
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        // Try to extract MessagingStyle data — this is where apps like WhatsApp,
        // SMS, and some others store the ACTUAL per-message sender+text array,
        // separate from the flat title/text fields which are often just a
        // collapsed summary ("New messages from 2 chats").
        val messagingLines = extractMessagingStyleLines(extras)

        // EXTRA_TEXT_LINES is another place some apps (esp. older-style
        // InboxStyle notifications) put multiple lines of real content.
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it.toString() }
            ?: emptyList()

        // EXTRA_BIG_TEXT sometimes holds a fuller version of the message
        // than EXTRA_TEXT (used in BigTextStyle notifications).
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // EXTRA_SUB_TEXT often holds sender/account context (e.g. email
        // account name, or WhatsApp business name).
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val combinedContent = buildString {
            if (messagingLines.isNotEmpty()) {
                append(messagingLines.joinToString("\n"))
            } else if (bigText != null && bigText.isNotBlank() && bigText != flatText) {
                append(bigText)
            } else if (textLines.isNotEmpty()) {
                append(textLines.joinToString("\n"))
            } else {
                append(flatText)
            }
        }

        Log.d(
            "NotifSpike",
            "App: $packageName | GroupSummary: $isGroupSummary | Title: $flatTitle | " +
                "SubText: $subText | Content: $combinedContent"
        )

        val entry = CapturedNotification(
            packageName = packageName,
            title = flatTitle,
            text = combinedContent,
            subText = subText,
            isGroupSummary = isGroupSummary,
            timestamp = sbn.postTime
        )
        NotificationStore.add(entry)

        sendBroadcast(Intent(ACTION_NEW_NOTIFICATION))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for the spike
    }

    /**
     * Extracts real per-message content from MessagingStyle notifications.
     * This is the style most modern messaging apps (WhatsApp, SMS, etc.) use.
     * The EXTRA_MESSAGES parcelable array carries sender + text + timestamp
     * per message, whether the notification is bundled/grouped or not.
     */
    private fun extractMessagingStyleLines(extras: android.os.Bundle): List<String> {
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return emptyList()

        return messages.mapNotNull { parcelable ->
            val bundle = parcelable as? android.os.Bundle ?: return@mapNotNull null
            val text = bundle.getCharSequence("text")?.toString() ?: return@mapNotNull null
            val senderName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val senderPerson = bundle.getParcelable("sender_person") as? android.app.Person
                senderPerson?.name?.toString()
            } else {
                bundle.getCharSequence("sender")?.toString()
            }
            if (senderName != null) "$senderName: $text" else text
        }
    }

    companion object {
        const val ACTION_NEW_NOTIFICATION = "com.example.notifspike.NEW_NOTIFICATION"
    }
}

data class CapturedNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val isGroupSummary: Boolean,
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
