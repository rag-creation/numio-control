# Control by Numio

A private, offline control panel for Android. Swipe in from the edge of the screen and your quick buttons, brightness and volume are there. Part of the Numio family, alongside Numio Calculator, Clock by Numio, Ezhuthola and the other Numio apps.

- No internet permission (the CI build fails if a release APK ever asks for it)
- No ads, analytics, tracking or accounts
- Settings stay on your phone
- Built for TalkBack from the start
- Free and open source (GPL-3.0)

> **Status: milestone 1 (in testing).** The panel opens from the screen edge or the accessibility button, with the Wi-Fi and Bluetooth pills, ten round buttons, brightness and volume sliders, and the power menu. The editor comes next.

## What each button can do

Android lets a normal app switch some things directly. For the rest it only lets apps *read* the state, so those buttons show whether they're on and open Android's own panel.

| Button | What happens | Needs |
|---|---|---|
| Flashlight | Switches on and off | Nothing |
| Sound | Ring → vibrate → silent | Do Not Disturb access, for silent |
| Auto rotate | Switches on and off | Change system settings |
| Brightness slider, auto brightness | Changes directly | Change system settings |
| Volume slider, mute | Changes media volume directly | Nothing |
| Lock, power menu, screenshot | Done by the accessibility service | The panel service |
| Wi-Fi, mobile data, NFC | Opens Android's own panel | Nothing |
| Bluetooth, airplane mode, location, screen cast, power saving | Opens the matching settings page | Nothing |

## What the accessibility service does (and doesn't)

The panel is an accessibility service because that's the only way an Android app can draw over other apps from the screen edge and lock the screen. Control by Numio asks for the least it can:

- **No accessibility events and `canRetrieveWindowContent="false"`**: it never sees what's on the screen, what you type or your notifications ([`control_service.xml`](app/src/main/res/xml/control_service.xml))
- It only draws two windows of its own: the thin edge handle, and the panel while it's open
- It uses three accessibility actions: lock screen, power menu, screenshot

### "Restricted setting" on Android 13 and newer

If you install the APK from a file, Android greys out the switch for its accessibility service. Open **App info → ⋮ → Allow restricted settings**, then turn it on in **Accessibility → Installed apps**. Apps installed from an app store such as F-Droid don't need this.

## Accessibility

- Every button is a real button or switch to TalkBack, with a label and its state ("Flashlight, off, switch")
- Buttons that open Android's panels say so ("Wi-Fi, on, double-tap to open Wi-Fi panel")
- Sliders can be moved with TalkBack's volume-key or swipe adjust
- The accessibility button / shortcut opens and closes the panel, so you don't need to find the edge handle
- Touch targets are at least 48 dp, and text follows the phone's font size
- Back closes the panel, and so does the Close button at its bottom

## Milestones

| # | Milestone | What it adds |
|---|-----------|--------------|
| 1 | **Panel** (this) | Accessibility service, edge handle with back-gesture exclusion, panel window with blur on supported phones, Wi-Fi/Bluetooth pills, 10-button grid, brightness and volume sliders, power menu, setup screen, dark / light / system, tests, CI |
| 2 | Editor | Rearrange, add and remove buttons and cards, with TalkBack "move earlier / later" actions |
| 3 | Music and counts | Music card with album art (notification access, optional), Calls and Messages counts |
| 4 | Looks | Ink theme, your own background picture, scene themes with RR's drawings, reduce motion, large buttons |
| 5 | Polish | App icon, About page and Numio easter egg, F-Droid listing |

Each milestone is its own pull request, tested on a real phone before merging.

## Architecture

- **Kotlin + Jetpack Compose + Material 3**, styled with the official Numio palette
- `service/`: `ControlService` (the accessibility service), `HandleView` (edge handle), `OverlayOwner` (gives Compose a lifecycle inside a service window)
- `panel/`: `PanelController` (reads the phone's state and carries out each button), `PanelScreen` (the panel UI), `PanelLayout` (which buttons, in which order; plain Kotlin, unit tested)
- `data/`: settings in DataStore (look, handle side and position, layout)
- `ui/`: the setup screen and the theme

Android 10 (API 29) is the minimum: the Wi-Fi, internet and NFC panels and the lock screen / screenshot actions all exist from there.

## Building

### Android Studio

1. **File → Open** this folder and let Gradle sync.
2. Pick the **app** run configuration and your phone, then **Run**.

Debug builds install as **Control Debug** with the app id `com.rr.numio.control.debug`, next to the real app.

### Terminal

```sh
./gradlew assembleDebug           # debug APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest       # unit tests → app/build/reports/tests/
./gradlew assembleRelease         # unsigned release APK (R8 on)
```

### GitHub test builds

Every pull request builds a debug APK, runs the tests, builds the release APK and checks its permissions. Download **ControlDebug.apk** from the run's **Artifacts** section.

Release APKs are signed by hand with the Numio key. The key is never added to GitHub.

## Licence and credits

Control by Numio is free software under the [GNU GPL v3.0 only](LICENSE).

- [Inter](https://github.com/rsms/inter) font, SIL Open Font License 1.1 ([licence](licenses/Inter-OFL.txt))
- The "Numio." wordmark uses letter shapes from [Chakra Petch](https://github.com/m4rc1e/Chakra-Petch), SIL Open Font License 1.1 ([licence](licenses/ChakraPetch-OFL.txt))
- A few icon shapes (Wi-Fi arcs, Bluetooth, lock shackle) follow [Lucide](https://github.com/lucide-icons/lucide), ISC licence ([licence](licenses/Lucide-ISC.txt))

Built by RR with help from Claude.
