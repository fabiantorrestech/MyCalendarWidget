package com.fabiantorrestech.mycalendarwidget.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.ui.theme.MyCalendarWidgetTheme
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.READ_CALENDAR] == true) {
            viewModel.refreshPermissionState(this)
        }
    }

    private val widgetId: Int
        get() = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.factory(applicationContext, widgetId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request calendar permissions
        val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        val hasPermission = permissions.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) {
            requestPermissionLauncher.launch(permissions)
        } else {
            viewModel.refreshPermissionState(this)
        }

        setContent {
            MyCalendarWidgetTheme(dynamicColor = viewModel.config.value.dynamicColor) {
                SettingsScreen(
                    viewModel = viewModel,
                    appWidgetId = widgetId,
                    onSaveComplete = {
                        refreshWidgets()
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun refreshWidgets() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val manager = GlanceAppWidgetManager(applicationContext)
            val ids = manager.getGlanceIds(BridgeCalWidget::class.java)
            ids.forEach { BridgeCalWidget().update(applicationContext, it) }
        }
    }
}
