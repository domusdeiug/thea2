package com.example.notifspike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var statusText: TextView
    private lateinit var adapter: ArrayAdapter<String>

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        listView = findViewById(R.id.notificationList)

        val grantButton: Button = findViewById(R.id.grantAccessButton)
        val clearButton: Button = findViewById(R.id.clearButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        grantButton.setOnClickListener {
            // Opens the system settings screen where the user must manually
            // enable notification access for this app. There is no normal
            // runtime permission dialog for this — it's always manual.
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        clearButton.setOnClickListener {
            NotificationStore.clear()
            refreshList()
        }

        statusText.text =
            "Step 1: Tap 'Grant Notification Access', find this app in the list, enable it.\n" +
            "Step 2: Come back here, send yourself a WhatsApp/TikTok/etc. notification.\n" +
            "Step 3: Watch this list fill in below."

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            refreshReceiver,
            IntentFilter(NotificationListener.ACTION_NEW_NOTIFICATION),
            Context.RECEIVER_NOT_EXPORTED
        )
        refreshList()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(refreshReceiver)
    }

    private fun refreshList() {
        val items = NotificationStore.getAll().map { n ->
            val time = DateFormat.format("HH:mm:ss", n.timestamp)
            "[$time] ${n.packageName}\nTitle: ${n.title}\nText: ${n.text}"
        }
        adapter.clear()
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
    }
}
