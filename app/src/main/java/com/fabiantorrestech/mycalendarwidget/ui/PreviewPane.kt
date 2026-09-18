package com.fabiantorrestech.mycalendarwidget.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import java.time.LocalDate

/** Which face the preview pane shows; the peek only exists for the density style. */
enum class PreviewView { WIDGET, PEEK }

/**
 * The one preview slot on the settings screen: a header with the pane's name and, for
 * Density, a switch in the top-right that swaps the widget face for the peek sheet so
 * the two share the space. The same composable is used pinned above the settings list
 * and inline inside it; when pinned, a tall agenda preview scrolls within the pane
 * rather than pushing the settings down.
 */
@Composable
fun PreviewPane(
    config: WidgetConfig,
    eventsByDay: Map<LocalDate, List<CalendarEvent>>,
    densitySnapshot: DensitySnapshot?,
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    cycleUiStyle: CycleUiStyle,
    use24Hour: Boolean,
    view: PreviewView,
    onViewChange: (PreviewView) -> Unit,
    scrollInside: Boolean,
    modifier: Modifier = Modifier
) {
    val isDensity = config.widgetStyle == WidgetStyle.DENSITY
    // The peek face only exists for Density; any other style always shows the widget.
    val shown = if (isDensity) view else PreviewView.WIDGET

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (shown) {
                    PreviewView.WIDGET -> "Widget Preview"
                    PreviewView.PEEK -> "Peek Preview"
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            if (isDensity) {
                TextButton(
                    onClick = {
                        onViewChange(
                            when (shown) {
                                PreviewView.WIDGET -> PreviewView.PEEK
                                PreviewView.PEEK -> PreviewView.WIDGET
                            }
                        )
                    }
                ) {
                    Text(
                        when (shown) {
                            PreviewView.WIDGET -> "Show peek"
                            PreviewView.PEEK -> "Show widget"
                        }
                    )
                }
            }
        }
        val cardModifier = if (scrollInside) Modifier.verticalScroll(rememberScrollState()) else Modifier
        when (shown) {
            PreviewView.WIDGET -> PreviewCard(
                config = config,
                eventsByDay = eventsByDay,
                profiles = profiles,
                activeProfileId = activeProfileId,
                cycleUiStyle = cycleUiStyle,
                densitySnapshot = densitySnapshot,
                use24Hour = use24Hour,
                modifier = cardModifier.padding(bottom = 4.dp)
            )
            PreviewView.PEEK -> PeekPreviewCard(
                config = config,
                eventsByDay = eventsByDay,
                use24Hour = use24Hour,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
