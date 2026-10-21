# Fan Remote

A dark-mode IR remote for the bedroom fan, for phones with an IR blaster (built for the Xiaomi Mi A2).
Kotlin + Jetpack Compose, no extra libraries, no internet permission.

## Build

**Android Studio:** File › Open › this folder, let Gradle sync, then Run ▶ with the phone plugged in
(USB debugging on). Or `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

**No Android Studio:** push this folder to a GitHub repo. The included workflow
(`.github/workflows/build.yml`) builds the APK on every push; download it from the run's
*Artifacts* section and install it on the phone (allow "Install unknown apps").

## How it sends

`ir/NecTransmitter.kt` turns each 32-bit code into an NEC frame for `ConsumerIrManager.transmit()`
at 38 kHz: 9000 µs mark, 4500 µs space, 32 bits MSB-first (560 µs mark + 560 µs space for 0,
1690 µs space for 1), then a 560 µs stop mark. Codes live in `ir/FanCommand.kt`.

## Notes

- IR is one-way, so the dial shows the app's last known state (saved between launches), not a
  reading from the fan. If they drift apart, tap a speed button to resync.
- Speed buttons and +/− also mark the fan as on, matching how most fans react.
- If nothing happens: point the top edge of the phone at the fan from within ~3 m. If it still
  doesn't respond, the codes may have been captured LSB-first; flip the bit loop in
  `NecTransmitter.pattern()` to `for (bit in 0..31)`.
