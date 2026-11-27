# Mi5G Debloat

Mi5G Debloat (Arabic: إزالة التطبيقات الزائدة) removes or disables selected
**system/preinstalled** packages for the current Android user, without root. It uses
[Shizuku](https://shizuku.rikka.app/) to run shell commands as the shell user.

Distributed via GitHub, not Google Play.

## Requirements

- Android 8.0 (API 26) or newer.
- [Shizuku](https://shizuku.rikka.app/download/) installed and running, with permission
  granted to this app. Root is never used or required.

## How to use

1. Install and start Shizuku, then grant Mi5G Debloat the Shizuku permission when asked.
2. Tap **Scan**. Scanning only uses Android's package manager and works without Shizuku.
3. On **Installed**, search or filter by tier, tick the apps you want to remove, and tap
   **Remove selected**. Select all affects only the visible, selectable rows.
4. Confirm the dialog. Removal runs for the current user only (user 0), so apps and data
   for other users are untouched.
5. Open **Uninstalled** to see the removal log and tap **Restore** (or **Restore all**) to
   undo. Restoration also needs Shizuku.

The home list scrolls as one page (status, scan, tabs, search, and rows), with the remove / restore-all button fixed at the bottom.

Only system/preinstalled packages appear on **Installed**. User-installed apps are hidden,
even when their package name is on the Recommended list: `pm uninstall --user 0` would
delete such an app completely, with no system-image copy to restore. Mi5G Debloat itself is
also hidden.

Packages have five tiers:

- **Recommended**: the original curated removal candidates.
- **Advanced**: optional vendor features; selectable by default with an extra confirmation.
- **Expert**, **Unknown**, and **Unsafe**: selectable only when **Allow unsafe selections**
  is enabled in Settings. This setting is off by default and shows a risk confirmation.

Unsafe packages are **disabled, never uninstalled**. Other tiers try per-user uninstall
first and fall back to disable-user when uninstall fails. Expert, Unknown, or Unsafe
selections can break the phone, cause a bootloop, or require a factory reset; use them at
your own risk and prefer disabling when unsure.

The app opens in Arabic (RTL) by default even if the phone's system language is English.
Use the language action on the home bar, or the language row on the **About** screen, to
switch between Arabic and English.

## Safety notes

- Restore from **Uninstalled**. Disabled packages use `pm enable`; removed packages use
  `install-existing`, with the `pm install-existing` fallback.
- If a removed app is not present on the system image, it cannot be restored from here.
  The app explains this and provides a Google Play button so you can reinstall it.
- You can also restore a system-image package manually with:

  ```sh
  adb shell cmd package install-existing --user 0 <pkg>
  ```

- Removing a preinstalled app may break a vendor feature. Use at your own risk.

## Credits

Inspired by [Android Phones Debloat Prompt](https://github.com/beebom-team/android-debloat-prompt)
by beebom-team (MIT License). Our package list and reasons are curated independently; they
are not copied from UAD-ng or Canta.

By [@midroid5G](https://github.com/midroid5G). Support: https://paypal.me/Midroid5G

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 @midroid5G.
