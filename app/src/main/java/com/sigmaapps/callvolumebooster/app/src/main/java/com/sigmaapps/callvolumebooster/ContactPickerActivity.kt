package com.sigmaapps.callvolumebooster

import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ContactPickerActivity : AppCompatActivity() {

    data class DeviceContact(val name: String, val number: String)

    private lateinit var adapter: ContactAdapter
    private var contacts: List<DeviceContact> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_picker)

        contacts = loadDeviceContacts()
        val listView = findViewById<android.widget.ListView>(R.id.contactsListView)
        val emptyText = findViewById<android.widget.TextView>(R.id.emptyContactsText)

        if (contacts.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        }

        adapter = ContactAdapter(contacts)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val contact = contacts[position]
            if (VipStore.isAdded(this, contact.number)) {
                VipStore.remove(this, contact.number)
                Toast.makeText(this, "Removed ${contact.name}", Toast.LENGTH_SHORT).show()
            } else {
                VipStore.add(this, contact.name, contact.number)
                Toast.makeText(this, "Added 1 of your contacts", Toast.LENGTH_SHORT).show()
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun loadDeviceContacts(): List<DeviceContact> {
        val result = mutableListOf<DeviceContact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val seen = mutableSetOf<String>()
            while (it.moveToNext()) {
                val name = if (nameIdx >= 0) it.getString(nameIdx) else null
                val number = if (numberIdx >= 0) it.getString(numberIdx) else null
                if (name != null && number != null) {
                    val normalized = VipStore.normalize(number)
                    if (normalized.isNotEmpty() && seen.add(normalized)) {
                        result.add(DeviceContact(name, number))
                    }
                }
            }
        }
        return result
    }

    private inner class ContactAdapter(private val items: List<DeviceContact>) :
        ArrayAdapter<DeviceContact>(this@ContactPickerActivity, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_contact, parent, false)

            val contact = items[position]
            view.findViewById<android.widget.TextView>(R.id.contactName).text = contact.name
            view.findViewById<android.widget.TextView>(R.id.contactNumber).text = contact.number

            val check = view.findViewById<android.widget.TextView>(R.id.contactCheck)
            check.visibility = if (VipStore.isAdded(this@ContactPickerActivity, contact.number)) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
            return view
        }
    }
}
