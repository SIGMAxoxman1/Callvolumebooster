package com.sigmaapps.callvolumebooster

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                onCorePermissionsGranted()
            }
            refreshList()
        }

    private val contactPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshList()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)

        findViewById<android.view.View>(R.id.addContactCard).setOnClickListener {
            contactPickerLauncher.launch(Intent(this, ContactPickerActivity::class.java))
        }

        findViewById<android.view.View>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupWatcherSwitch()

        if (hasCorePermissions()) {
            onCorePermissionsGranted()
        } else {
            requestCorePermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        allowStarredContactsThroughDnd()
    }

    private fun setupWatcherSwitch() {
        val switch = findViewById<android.widget.Switch>(R.id.watcherSwitch)
        val statusText = findViewById<android.widget.TextView>(R.id.watcherStatusText)

        switch.isChecked = VipStore.isWatcherEnabled(this)
        updateWatcherStatusText(statusText, switch.isChecked)

        switch.setOnCheckedChangeListener { _, isChecked ->
            VipStore.setWatcherEnabled(this, isChecked)
            updateWatcherStatusText(statusText, isChecked)
            if (isChecked) {
                if (hasCorePermissions()) startWatchingService()
            } else {
                stopService(Intent(this, CallWatchService::class.java))
            }
        }
    }

    private fun updateWatcherStatusText(view: android.widget.TextView, running: Boolean) {
        view.text = if (running) {
            "Running — watching for VIP calls."
        } else {
            "Turned off — VIP calls will ring normally."
        }
    }

    private fun hasCorePermissions(): Boolean {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCorePermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    /**
     * Once the core permissions are granted, the app starts watching for
     * calls immediately (if the switch in the side menu is on) and asks for
     * the two extra permissions that make the volume boost reliable:
     * Do Not Disturb access (so the boost still works in silent/DND mode)
     * and battery-optimization exemption (so Android doesn't kill the
     * background service).
     */
    private fun onCorePermissionsGranted() {
        if (VipStore.isWatcherEnabled(this)) {
            startWatchingService()
        }
        requestNotificationPolicyAccessIfNeeded()
        requestIgnoreBatteryOptimizationsIfNeeded()
        allowStarredContactsThroughDnd()
    }

    /**
     * This is the real fix for missed VIP calls during Do Not Disturb:
     * Android's Telecom system decides whether to ring *before* our app
     * ever gets a broadcast about the call, based on this policy — not on
     * anything we can react to afterwards. Once VIP contacts are starred
     * (see ContactPickerActivity) and this policy is set, Android itself
     * lets their calls ring, even in Priority-only Do Not Disturb mode.
     */
    private fun allowStarredContactsThroughDnd() {
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        if (notificationManager?.isNotificationPolicyAccessGranted == true) {
            val current = notificationManager.notificationPolicy
            val categories = current.priorityCategories or
                android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
            notificationManager.notificationPolicy = android.app.NotificationManager.Policy(
                categories,
                android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                current.priorityMessageSenders
            )
        }
    }

    private fun startWatchingService() {
        val intent = Intent(this, CallWatchService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun requestNotificationPolicyAccessIfNeeded() {
        val notificationManager =
            getSystemService(android.app.NotificationManager::class.java)
        if (notificationManager?.isNotificationPolicyAccessGranted != true) {
            android.widget.Toast.makeText(
                this,
                "Turn on Do Not Disturb access for Call Volume Booster on the next screen — otherwise VIP calls can't get through silent mode.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    private fun requestIgnoreBatteryOptimizationsIfNeeded() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun refreshList() {
        val container = findViewById<android.widget.LinearLayout>(R.id.contactsListContainer)
        val emptyText = findViewById<android.widget.TextView>(R.id.emptyStateText)
        container.removeAllViews()

        val contacts = VipStore.getAll(this)
        emptyText.visibility = if (contacts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        for (contact in contacts) {
            val row = layoutInflater.inflate(R.layout.item_contact, container, false)
            row.findViewById<android.widget.TextView>(R.id.contactName).text = contact.name
            row.findViewById<android.widget.TextView>(R.id.contactNumber).text = contact.number
            row.findViewById<android.widget.TextView>(R.id.contactCheck).visibility = android.view.View.VISIBLE
            container.addView(row)
        }
    }
}
