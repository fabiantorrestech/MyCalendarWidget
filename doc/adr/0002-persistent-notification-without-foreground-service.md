# 2. Persistent notification without a foreground service

Date: 2026-10-01

## Status

Accepted

## Context

The density widget is to be offered as an always-present notification: the headline and strip when collapsed, plus an agenda list when the user expands it. It has to stay current (the "now" caret, the "next in 25m" qualifier, calendar edits, the midnight rollover) and it has to stay posted. Since Android 14, users can swipe away even ongoing notifications, including those that belong to a foreground service, so no mechanism makes the notification truly undismissable. A foreground service would keep the process alive, but it needs the `FOREGROUND_SERVICE` permissions, a `specialUse` type that Google Play reviews, and it costs battery for a display that only needs to change every few minutes. The app already refreshes its widgets with inexact `AlarmManager` alarms and the calendar `PROVIDER_CHANGED` broadcast, without WorkManager.

## Decision

We will post an ordinary ongoing, silent notification from a broadcast receiver, with no foreground service. Its delete intent goes to the same receiver, which re-posts it when the user swipes it away. The notification is refreshed by its own five-minute inexact `AlarmManager` alarm, by the calendar-change, date-change and midnight broadcasts the widgets already use, and by `BOOT_COMPLETED`. WorkManager is not added. The only new permissions are `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED`.

## Consequences

There is no long-running process, no Play foreground-service declaration, and no new dependency, and refresh shares the widgets' cadence and receivers. The notification can lag by up to the inexact alarm's slack (a few minutes) and is only as fresh as the `PROVIDER_CHANGED` broadcast is reliable on a given device. Aggressive OEM battery managers that kill alarms will leave it stale until the next trigger. Swiping it away re-posts it after a brief flicker rather than preventing the swipe, so turning it off has to happen in the app. Moving to a foreground service later means a service component, new permissions and a Play policy declaration, and that, not the rendering code, is the expensive part to reverse.
