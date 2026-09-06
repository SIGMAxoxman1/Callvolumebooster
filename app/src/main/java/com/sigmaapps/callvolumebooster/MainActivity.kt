package com.sigmaapps.callvolumebooster

import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { handlePickedContact(it) }
            }
        }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            updateStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("vip_prefs", MODE_PRIVATE)

        findViewById<android.widget.Button>(R.id.pickContactButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        }

        findViewById<android.widget.Button>(R.id.grantPermissionsButton).setOnClickListener {
            requestNeededPermissions()
        }

        findViewById<android.widget.Button>(R.id.startServiceButton).setOnClickListener {
            if (hasNeededPermissions()) {
                val intent = Intent(this, CallWatchService::class.java)
                ContextCompat.startForegroundService(this, intent)
                android.widget.Toast.makeText(this, "Watching for calls now.", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "Grant permissions first.", android.widget.Toast.LENGTH_SHORT).show()
                requestNeededPermissions()
            }
        }

        // Ask for permissions right away instead of waiting for a button press.
        requestNeededPermissions()

        updateStatus()
    }

    private fun hasNeededPermissions(): Boolean {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handlePickedContact(contactUri: Uri) {
        contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val number = if (numberIndex >= 0) cursor.getString(numberIndex) else null
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                if (number != null) {
                    prefs.edit()
                        .putString("vip_number", normalizeNumber(number))
                        .putString("vip_name", name)
                        .apply()
                }
            }
        }
        updateStatus()
    }

    private fun normalizeNumber(number: String): String {
        // Keeps digits only, so formatting differences (spaces, dashes) don't break matching.
        return number.filter { it.isDigit() }.takeLast(10)
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun updateStatus() {
        val name = prefs.getString("vip_name", null)
        val statusView = findViewById<android.widget.TextView>(R.id.statusText)
        statusView.text = if (name != null) {
            "VIP contact: $name"
        } else {
            "No VIP contact selected yet."
        }
    }
}
