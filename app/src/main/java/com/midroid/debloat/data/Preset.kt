package com.midroid.debloat.data

fun applyPreset(items: List<AppItem>, catalog: OemCatalog, selected: OemId): Set<String> = items
    .asSequence()
    .filter { it.isSystem }
    .filter { tierOf(it.pkg, catalog, selected) == Tier.RECOMMENDED }
    .map { it.pkg }
    .toSet()
