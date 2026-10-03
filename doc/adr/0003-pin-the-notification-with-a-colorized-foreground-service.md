# 3. Pin the notification with a colorized foreground service

Date: 2026-10-02

## Status

Accepted

Supersedes [2. Persistent notification without a foreground service](0002-persistent-notification-without-foreground-service.md)

## Context

The persistent density notification was an ordinary ongoing notification at the highest priority an app may request. In use it still got lost: every messaging conversation and every fresh alert sorted above it, and the user wants it to stay at the top of the shade, below only real phone calls. The app is sideloaded and will not be submitted to Google Play, so Play's foreground-service policy does not constrain it. Faking a call is not an option: Android grants the call boost only to the default phone app and forces the call template, which would drop the strip and the agenda. A throwaway spike on the Android 16 emulator (branch `spike-colorized-fgs`) showed that a colorized notification belonging to a running foreground service sorts in its own section above Messages conversations and every ordinary notification, and below an ongoing call; a message that has only just arrived floats above it for a few seconds before settling below it.

## Decision

We will offer a "Pinned above messages" placement, the default, in which the notification is the foreground notification of a `specialUse` foreground service and is colorized in the accent-container tone. The service does no work of its own: it only holds the notification, rebuilt by the same code as before, and every existing trigger still refreshes it. Because Android allows a foreground service to start only from the foreground or a few exempt moments, it is started from the app, after boot, after an app update and from the Repost button; a refresh that finds it stopped and cannot start it posts the notification unpinned instead. The Top and Silent placements remain and use no service. The rest of ADR 0002 stands: AlarmManager refreshes, no WorkManager, re-posting on swipe.

## Consequences

The notification now stays above conversations and ordinary alerts, which is the point. In exchange the app keeps a process alive while pinned, costing some memory but next to no CPU, and appears under "Active apps" in Quick Settings, where the user can stop it; until the next app start, boot or Repost it then falls back to the unpinned look. The colorized background means the Today pill and the + button need a stronger accent so they do not vanish into it. The placement depends on how Pixel's system UI sections colorized foreground-service notifications, which could change in a future Android release or differ on other manufacturers' builds. Undoing this means removing the service, its two permissions and the placement option, all contained in the notification package.
