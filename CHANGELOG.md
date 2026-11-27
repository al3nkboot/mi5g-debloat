# Changelog

All notable releases of Mi5G Debloat. Versions follow the app `versionName`.

## 0.2.2 — 2026-11-27

- Fixed the in-app language switch so English and Arabic apply immediately to the whole UI, persist across restarts, and update text direction.

## 0.2.1 — 2026-11-27

- The brand list is a dropdown that always shows the selected list. The detected brand stays as a separate label. The previous horizontal chips could be mis-tapped while scrolling, which made ColorOS packages look unapplied even though the list was working.
- Arabic Settings and confirmation text uses خبير / غير مصنف / خطرة instead of English tier names.

## 0.2.0 — 2026-11-27

- Ready-made package lists for Samsung (One UI), Xiaomi/Redmi/POCO (HyperOS/MIUI), Realme/OPPO/OnePlus (ColorOS), Honor/Huawei (MagicOS/EMUI), vivo/iQOO (Funtouch/OriginOS), and Google/AOSP plus partner stubs.
- Auto-detects the phone maker; you can switch brand or All.
- **Select recommended** ticks only Recommended apps for review. Nothing is removed automatically.
- Lists are independently sourced; they are not copied from UAD-ng or Canta.

## 0.1.4 — 2026-11-27

- Home screen scrolls as one list (status, scan, tabs, search, and rows), with Remove / Restore all fixed at the bottom.
- Tier chips show counts; long labels use ellipsis; Shizuku ready state shows a check icon.
- Release signing can be loaded from a local properties file or environment variable. Keystores stay off the repository.

## 0.1.3 — 2026-11-27

- Installed tab lists system/preinstalled apps only. User-installed apps are hidden.
- Five tiers: Recommended, Advanced, Expert, Unsafe, Unknown.
- Canta-like tabs, search, and tier filters.
- Unsafe packages are disabled, never uninstalled, and stay behind a Settings toggle.

## 0.1.2 — 2026-11-26

- Uninstall falls back to `pm disable-user` when it fails, and the failure reason is shown.
- Restore tries `cmd package install-existing`, then `pm install-existing`.
- Status messages follow the in-app language. Count strings use Arabic plural forms.

## 0.1.1 — 2026-11-26

- Curated Realme/ColorOS safe list.
- Arabic is the default in-app language, with a language switcher.

## 0.1.0-m1 — 2026-11-26

- First milestone: Shizuku setup, scan, classify, safe removal, and a restore log.
