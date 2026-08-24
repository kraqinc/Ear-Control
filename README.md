# EarControl

A clean Android control center for Bluetooth earbuds.

## Included
- Apple-inspired Compose UI.
- Left / Right / Case battery cards (real values when Android/device exposes them; safe demo fallback otherwise).
- System media volume control.
- Left/right balance control in the UI.
- Bluetooth permission flow and paired-device discovery.
- GitHub Actions workflow that builds a debug APK.
- Architecture placeholder for deeper vendor/root integrations without making root mandatory.

## Important battery note
Android's Bluetooth stack can report a device battery level, but per-earbud and charging-case values are not uniformly exposed to third-party apps. Vendor support varies. This project keeps the UI ready for L/R/case providers and does not fake a claim of universal support.

## Klavika Bold
Klavika is not bundled because it is a proprietary typeface. Put your licensed `klavika_bold.ttf` in `app/src/main/res/font/` and wire `FontFamily(Font(R.font.klavika_bold))` in `MainActivity.kt`.

## Build

```bash
./gradlew :app:assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`
