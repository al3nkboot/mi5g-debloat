# Contributing

Thank you for helping improve Mi5G Debloat. Please open an issue before a large pull request.

## Suggest a package

Use the **Package suggestion** issue form. Every **Recommended** entry needs **at least two independent public sources** that identify the package and support removing or disabling it.

Do **not** copy lists, ratings, or descriptions from:

- Universal Android Debloater Next Generation (UAD-ng, GPL)
- Canta (LGPL)
- Poda or any other reprint of those lists

Vendor docs, first-hand XDA/forum reports, and permissively licensed write-ups are fine. If you are unsure, propose **Advanced** or **Expert**, not Recommended.

Keep these in **Unsafe**: setup, account, security, telephony, the default launcher, System UI, OTA, payment/keystore, and Knox / Mi service cores.

## Code and docs

- Keep changes small. Match the existing Kotlin and Compose style.
- If you add or rename a string, update both `values/strings.xml` and `values-ar/strings.xml` (Arabic plural forms need all six CLDR quantities).
- Run `./gradlew :app:testDebugUnitTest`.
- Do not commit keystores, passwords, or `local.properties`.
- Do not include personal names or private data in issues, commits, or docs. Use `@midroid5G` only.
