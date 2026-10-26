# 4. Flip the density widget to its agenda with a RemoteViews ViewFlipper

Date: 2026-10-26

## Status

Accepted

## Context

The user often forgets to tap the density widget to see the agenda, and asked for the widget to show its agenda on a timed cycle, the way the widget stacks in their Shortcut Hub app rotate. A home-screen widget learns nothing when the launcher becomes visible again after another app, so a cycle the app drives itself would mean waking every few seconds, which the battery work in this branch's parent set out to avoid. The widget is built with Glance 1.1.1, which has no flipper or auto-advancing component. Android's RemoteViews does have one, `ViewFlipper`, which the launcher process animates on its own while the widget is on screen and pauses when it is not. Glance can host raw RemoteViews through `AndroidRemoteViews`, placing its own composed children inside a container view. Until now the density widget read no event text while its peek was closed (rule G1); a page that shows titles without a tap breaks that rule while it is on.

## Decision

We will build the "Flip to the agenda on a timer" option by composing the density widget, while the option is on and the peek is closed, as an `AndroidRemoteViews` whose container is a `ViewFlipper` (`res/layout/density_flipper.xml`, or `density_flipper_still.xml` without the fade for E-ink). Its two children are the ordinary strip page and an agenda page drawn with the peek's own rows, and the flip interval comes from the widget's setting through `setFlipInterval`. The agenda page does not scroll: a spike on the Android 16 emulator showed that a collection view (the peek's `LazyColumn`) inside `AndroidRemoteViews` renders empty, with RemoteViews logging that `setRemoteAdapter` is only for app widgets, so the page shows the rows that fit, at most ten, in a plain column. The app schedules nothing for it; the launcher does the flipping. While the option is on, the widget loads event titles on every render so the agenda page has them, which relaxes G1 for that option only. The option is off by default, and the wake cycle and the tap-to-peek keep working as before.

## Consequences

The agenda now comes into view without a tap and without the app waking up, so the feature costs no battery beyond the calendar query each render already makes. In exchange the density widget depends on Glance's `AndroidRemoteViews` bridge and on how each launcher hosts a `ViewFlipper`; a launcher that never makes the widget visible in the usual way may never flip, and an e-ink screen pays a refresh on every flip. Every redraw rebuilds the flipper's children, which puts it back on the strip page, so a five-minute tick can cut an agenda page short. The agenda page cannot scroll, so a busy day shows only its first rows there; the tap-to-peek list stays the way to see the rest. With the option on, a widget on the home screen reads event titles all the time, which matters to anyone relying on the old content-free behaviour. Reversing it means deleting `DensityFlip`, the two layouts, the flipper branch in `BridgeCalWidgetContent` and the two settings, all contained to the widget and its settings.
