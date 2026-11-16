# Olauncher 200 personal build

Based on `minaric0/Olauncher` at `260697336e07cd39afdae1ef257fb8e75621952f`.
App label: **Olauncher 200**. Package: `app.olauncher.personal200`.
Version: `v6.11.5-200.1`, version code `118`.

## Readability at 200 × 200 / approximately 20 dpi

- Text dimensions are twice the original sizes, including settings, popup menus,
  app actions, dialogs, the clock, and app labels. The text-size setting shows
  **200%** by default and allows **150–400%** relative to the upstream dimensions.
- The default font uses a medium weight. Text shadows and fading text edges are
  removed so small letter strokes are easier to distinguish. The bold-font
  setting can switch back to the regular system font.
- On windows with a shortest edge of at most 320 pixels and density below 80 dpi,
  the activity uses a local density floor of 80 dpi. At 200 × 200 / 20 dpi and
  default text adjustment, this gives a 400 × 400 dp launcher viewport, 14-pixel
  settings/popup text, 26-pixel app labels, and 12-pixel app-action labels.
  The system-wide density and other apps are not modified.
- Settings labels and values use separate lines. Tapping the row activates its
  control. App action menus scroll horizontally to retain the full button names.
- Small windows have visible Apps and Settings buttons on home and a Home button
  in the drawer and settings. The home list scrolls if all configured apps do not
  fit. Portrait and landscape use the same compact-friendly layout.
- Four home slots remain the default. The clock and automatic keyboard opening
  start disabled on small windows to leave room for apps; both remain configurable.

Text still renders within the available 200 × 200 pixels; this build cannot add
detail beyond that resolution or control Samsung's final display upscaling.

## Build and validate

Use JDK 17, Android SDK platform 36, and Android build tools 35.0.0. Configure
`ANDROID_HOME` or an untracked `local.properties`, then run:

```sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintRelease
```

The additional Robolectric tests render Android layouts at 200 × 200, check
light/dark and Korean settings, verify nonoverlapping controls, check drawer
actions and home scrolling, and exercise extreme-density configuration without
modifying the source configuration. Rendered checks are written under
`app/build/reports/compact-ui/`.

Release output is `app/build/outputs/apk/release/app-release-unsigned.apk`.
The delivered APK is separately aligned and release-signed with the personal key.
The signing key and password are distributed privately in the signing backup,
never in this public repository. Future updates need the same key, the same
application ID, and a higher version code.

## Install

Install the delivered signed APK normally or with `adb install -r APK_PATH`.
Select **Olauncher 200** as the default home app when Android asks, or use the
Set as default launcher control in its settings. The personal application ID
allows the original Olauncher to remain installed alongside this build.

Changes and corresponding source are available under the repository's GPLv3
license. Runtime rendering on a physical Samsung/One UI device requires a device
check; the automated rendering checks run on Android API 35.
