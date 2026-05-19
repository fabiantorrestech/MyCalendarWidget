package com.fabiantorrestech.mycalendarwidget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.fabiantorrestech.mycalendarwidget.ui.SettingsActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}