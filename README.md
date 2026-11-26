# Mi5G Debloat

Mi5G Debloat (Arabic: إزالة التطبيقات الزائدة) removes a small, hand-curated set of
preinstalled bloatware and ad/telemetry stubs from an Android phone **for the current
user**, without root. It uses [Shizuku](https://shizuku.rikka.app/) to run shell commands
as the shell user.

Distributed via GitHub, not Google Play.

## Requirements

- Android 8.0 (API 26) or newer.
- [Shizuku](https://shizuku.rikka.app/download/) installed and running, with permission
  granted to this app. Root is never used or required.

## How to use

1. Install and start Shizuku, then grant Mi5G Debloat the Shizuku permission when asked.
2. Tap **Scan**. Scanning only uses Android's package manager and works without Shizuku.
3. On the **Safe** tab, tick the apps you want to remove and tap **Remove selected**.
   The **User apps** and **Protected** tabs are read-only.
4. Confirm the dialog. Removal runs for the current user only (user 0), so your apps and
   data for other users are untouched.
5. Use the **Log** screen to see what was removed and tap **Restore** (or **Restore all**)
   to undo. Restoration also needs Shizuku.

The safe list is **our own list, curated independently**. In addition to the original
entries, it now covers common realme/ColorOS/OPPO HeyTap packages such as Lock Screen
Magazine, App Market, Game Space, Theme Store and realme Community. Each entry shows a
one-line reason. The app never removes packages outside that list, and refuses
system-critical prefixes even if they appear on a custom list.

The app opens in Arabic (RTL) by default even if the phone's system language is English.
Use the language action on the home bar, or the language row on the **About** screen,
to switch between Arabic and English.

## Safety notes

- Removal is per-user and reversible. From the app, restore from the **Log** screen.
- You can also restore manually with:

  ```sh
  adb shell cmd package install-existing --user 0 <pkg>
  ```

- Removing a preinstalled app may break a vendor feature. Use at your own risk.

## Credits

Inspired by [Android Phones Debloat Prompt](https://github.com/beebom-team/android-debloat-prompt)
by beebom-team (MIT License). Our safe list was curated independently and is not copied
from any other project's list.

By [@midroid5G](https://github.com/midroid5G). Support: https://paypal.me/Midroid5G

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 @midroid5G.
