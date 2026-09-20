# Density widget style — what changed

Everything below is on the `NonStressfulWidget` branch and is visible to someone
using the app. Grouped by where you see it, newest behaviour described rather
than the order it was built in.

## The widget itself

- New fourth widget style, **Density**, which draws the day as one bar instead of a list of events.
- Density never reads event titles while it sits on the home screen; it only counts and measures.
- Three colour schemes for the bar: **Shape** (a plain yes/no that something is happening), **Tonal** (one colour, shades per calendar, events at the same time still split apart) and **Detail** (each event in its own calendar colour).
- Tonal assigns automatic shades so neighbouring calendars land as far apart on the ramp as it allows; a calendar keeps its shade when another calendar is added.
- Tonal shades can also be assigned per calendar by hand, with a "Reset to automatic".
- Headline status in large text, in one of three counts: **Left today**, **Left of total** or **Total**.
- Second line names the day and what is next: "Thu (9/18) · next at 4:15", with the date in parentheses and bold while the featured day is today.
- Hour axis under the bar with four tick labels, honouring your 12/24-hour setting.
- A red caret marks "now" on today's bar, breaking slightly above and below the track so it reads over a busy block.
- Outside the day's window the caret pins to the nearer edge instead of disappearing, so an early riser still sees where they are.
- Look-ahead bars for the next few days, each with its weekday and date ("Fri (9/18)"); the weekday drops on narrow columns so nothing clips.
- Coinciding events stack into at most two lanes; a third shares the last lane.
- An all-day event draws a thin band along the top of that day's bar, on today's bar and on each look-ahead bar, without touching the per-mode colours.
- Today's bar grows to fit the day: an event earlier or later than your set window extends the bar to the whole hour that covers it, and the axis ticks follow.
- An event crossing midnight is split at midnight: today's bar ends flush with a small chevron, the next day's begins from the left edge with a matching one.
- After 19:00 with nothing left today, the widget rolls over and shows tomorrow.
- Chrome on the widget: a small refresh circle, the open-calendar button and a quick-add (+), each following its setting; they move to their own row or collapse to one button as the widget narrows.
- Density uses only the selected profile, so the widget draws no profile switcher.
- Density refreshes every five minutes and at local midnight.

## Tapping the widget (the peek sheet)

- Tapping anywhere on the bar area opens a scrolling list of what is coming up.
- Two layouts: **Grouped** (a header per day) or **Dated rows** (a date pill on every row).
- Day headers are bold; today's sits on a filled pill so it stands out in grayscale too.
- Today's events that have already finished stay on the list, faded, above what is still ahead.
- The sheet says "Loading…" until the calendar query answers, instead of claiming there is nothing coming up.
- The "Upcoming ×" bar stays pinned at the top while the list scrolls, so it can be closed from anywhere.
- The sheet also closes from any date header, and lapses on its own after five minutes.
- Behind the sheet the bar is dimmed and blurred rather than hidden.

## Settings

- **Widget Style** is the first control in Appearance for every style.
- Choosing Density starts it on the Tonal scheme; switching away and back keeps whatever you picked.
- Density's look controls (colour scheme, peek layout, header status) sit directly under the style, each with a short explanation.
- Only controls the selected style actually uses are shown; the rest are hidden rather than left inert.
- Font-size sliders and per-category fonts are named after what the selected style draws with them (Daily items left, Peek date headers, Following days bars, Peek event names, Date & next-event line).
- **Widget Behavior** holds Today start/end time, look-ahead bars, load baseline, show-tomorrow-after and peek horizon, each with a description of what it does.
- The preview pins above the settings while you change them, capped at about 40% of the screen; a button in its corner swaps between the widget view and the peek view.
- "Sticky preview to top" can be turned off to put the preview back into the list; the choice is remembered.
- Previews run on a scripted sample calendar (a busy today with a collision and an all-day, a light day, an over-capacity day, a medium day), so nothing private is shown and the shapes are always legible.
- Both preview cards are outlined so their edges read against the settings background.
- Material You sits directly above **Busy color**, whose description says what it paints, and notes in Detail that the day bar uses calendar colours instead.
- Under Density the profile chips and Profiles section are hidden, with one red note explaining that Density uses only the selected profile.
- **Reset all defaults** at the bottom, in red, asks "Are you sure?" and then returns every setting of that widget to defaults: all profiles, the widget name and any sync link.
- **Auto-backup**: pick a folder once, and every tap of Done writes one JSON per placed widget into it, linked widgets included; each file still imports through "Import Config".
- New widgets are placed on the Density style, on the Tonal scheme, with the purple busy colour.

## Fixes worth knowing about

- Launchers that never report a widget size (Inkos, for one) made the widget think it was one row tall and drop the axis and bars; it now assumes a full-width size for those launchers. Set the launcher's own widget height to about 140dp or more.
- The refresh button used to re-render without actually reloading; it now updates the store the widget reads, for every style.
- All-day events used to flag the day before as well, because the calendar stores them in UTC; they now flag only the days they are dated on.
- The hour axis scales with the same slider as the day labels, so one control sizes every hour and day label.
- The seam between two stacked events showed a lighter line in dark mode; it now matches the outline in both themes.
- The look-ahead bars distinguish a day at the baseline from one past it: an over-capacity day fills completely, turns heavier and carries a +.
- The settings screen no longer renders the profile chips, the Profiles section and the add-profile dialog twice.
