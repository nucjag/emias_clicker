package com.example.autoclicker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etSearchText: EditText = findViewById(R.id.etSearchText)
        val cbNegative: CheckBox = findViewById(R.id.cbNegative)
        val etDelay: EditText = findViewById(R.id.etDelay)
        val btnEnableAccessibility: Button = findViewById(R.id.btnEnableAccessibility)
        val btnShowFloatingButton: Button = findViewById(R.id.btnShowFloatingButton)

        val prefs = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)

        // Загрузить настройки
        etSearchText.setText(prefs.getString("searchText", "Бабай"))
        cbNegative.isChecked = prefs.getBoolean("negative", false)
        etDelay.setText(prefs.getLong("delay", 500).toString())

        // Авто-сохранение поискового текста
        etSearchText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("searchText", s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        cbNegative.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("negative", isChecked).apply()
        }

        // Валидация и сохранение задержки
        etDelay.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim()
                val delay = validateDelay(input)

                if (delay != null) {
                    prefs.edit().putLong("delay", delay).apply()
                } else if (input.isNotEmpty()) {
                    Toast.makeText(this@MainActivity,
                        "Ошибка: введите число от 1 до 50000",
                        Toast.LENGTH_SHORT).show()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnShowFloatingButton.setOnClickListener {
            if (checkOverlayPermission()) {
                startService(Intent(this, FloatingButtonService::class.java))
                Toast.makeText(this, "Висячая кнопка запущена", Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun validateDelay(input: String): Long? {
        return try {
            val delay = input.toLong()
            if (delay in 1..50000) delay else null
        } catch (e: Exception) {
            null
        }
    }

    private fun checkOverlayPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                100
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && checkOverlayPermission()) {
            startService(Intent(this, FloatingButtonService::class.java))
            Toast.makeText(this, "Висячая кнопка запущена", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
        }
    }
}