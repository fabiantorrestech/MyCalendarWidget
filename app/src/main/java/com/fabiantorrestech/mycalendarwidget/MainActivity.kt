package com.fabiantorrestech.mycalendarwidget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.app.NotificationManagerCompat
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import com.fabiantorrestech.mycalendarwidget.ui.NotificationSettingsCard
import com.fabiantorrestech.mycalendarwidget.ui.NotificationSettingsViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.WidgetSummary
import com.fabiantorrestech.mycalendarwidget.ui.SettingsActivity
import com.fabiantorrestech.mycalendarwidget.ui.WidgetListViewModel
import com.fabiantorrestech.mycalendarwidget.ui.theme.MyCalendarWidgetTheme

class MainActivity : ComponentActivity() {

    private val widgetListViewModel: WidgetListViewModel by viewModels()
    private val notificationViewModel: NotificationSettingsViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableNotification() else showNotificationsBlocked()
    }

    /** Asks for POST_NOTIFICATIONS first on Android 13+, then turns the notification on. */
    private fun requestNotificationOn() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        when {
            needsPermission -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !NotificationManagerCompat.from(this).areNotificationsEnabled() -> showNotificationsBlocked()
            else -> enableNotification()
        }
    }

    /**
     * Follows the widget already chosen if it is still placed, else the first density
     * widget, else the first widget of any style.
     */
    private fun enableNotification() {
        val widgets = widgetListViewModel.widgets.value
        val current = notificationViewModel.prefs.value.followedWidgetId
        val target = widgets.firstOrNull { it.appWidgetId == current }
            ?: widgets.firstOrNull { it.style == WidgetStyle.DENSITY }
            ?: widgets.firstOrNull()
            ?: return
        notificationViewModel.enable(target.appWidgetId)
    }

    /** The card's "Repost notification": only possible while notifications are allowed. */
    private fun repostNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            showNotificationsBlocked()
            return
        }
        notificationViewModel.repost()
        Toast.makeText(this, "Notification reposted", Toast.LENGTH_SHORT).show()
    }

    private fun showNotificationsBlocked() {
        Toast.makeText(
            this,
            "Notifications are off for BridgeCal. Turn them on in system settings.",
            Toast.LENGTH_LONG
        ).show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyCalendarWidgetTheme {
                var showWidgetList by rememberSaveable { mutableStateOf(false) }
                val widgets by widgetListViewModel.widgets.collectAsState()
                val notificationPrefs by notificationViewModel.prefs.collectAsState()
                val sheetState = rememberModalBottomSheetState()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    widgetListViewModel.load()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("BridgeCal") },
                            actions = {
                                IconButton(onClick = { showWidgetList = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = "Manage widgets"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    OnboardingScreen(modifier = Modifier.padding(innerPadding)) {
                        NotificationSettingsCard(
                            prefs = notificationPrefs,
                            widgets = widgets,
                            onEnabledChange = { on ->
                                if (on) requestNotificationOn() else notificationViewModel.disable()
                            },
                            onFollow = notificationViewModel::follow,
                            onRowTapChange = notificationViewModel::setRowTap,
                            onPlacementChange = notificationViewModel::setPlacement,
                            onPagingModeChange = notificationViewModel::setPagingMode,
                            onShowAddButtonChange = notificationViewModel::setShowAddButton,
                            onShowRefreshButtonChange = notificationViewModel::setShowRefreshButton,
                            onLockScreenChange = notificationViewModel::setLockScreen,
                            onRepost = ::repostNotification
                        )
                    }

                    if (showWidgetList) {
                        ModalBottomSheet(
                            onDismissRequest = { showWidgetList = false },
                            sheetState = sheetState
                        ) {
                            WidgetListContent(
                                widgets = widgets,
                                onWidgetTap = { widgetId ->
                                    val intent = Intent(context, SettingsActivity::class.java)
                                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                    context.startActivity(intent)
                                    showWidgetList = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        widgetListViewModel.load()
    }
}

@Composable
private fun WidgetListContent(
    widgets: List<WidgetSummary>,
    onWidgetTap: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Your Widgets",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (widgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No widgets placed yet.\nLong-press your home screen to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn {
                items(widgets, key = { it.appWidgetId }) { widget ->
                    WidgetListItem(widget = widget, onClick = { onWidgetTap(widget.appWidgetId) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun WidgetListItem(widget: WidgetSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = widget.name.ifBlank { "Widget ${widget.displayIndex}" },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = widget.style.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (widget.syncSourceId != null) {
            AssistChip(
                onClick = {},
                label = { Text("Synced", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingScreen(
    modifier: Modifier = Modifier,
    belowSteps: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "BridgeCal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A calendar widget for your home screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How to get started",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OnboardingStep(number = "1", text = "Long-press your home screen")
                OnboardingStep(number = "2", text = "Tap Widgets")
                OnboardingStep(number = "3", text = "Find BridgeCal and drag it onto your screen")
                OnboardingStep(number = "4", text = "Tap Configure to customize it")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        belowSteps()
    }
}

@Composable
private fun OnboardingStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(28.dp)
                .padding(4.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
