package com.midroid.debloat.data

import androidx.annotation.StringRes
import com.midroid.debloat.R

@StringRes
fun reasonResource(key: String): Int = when (key) {
    "ads" -> R.string.reason_ads
    "partner_stub" -> R.string.reason_partner_stub
    "social" -> R.string.reason_social
    "oem_store" -> R.string.reason_oem_store
    "oem_browser" -> R.string.reason_oem_browser
    "oem_themes" -> R.string.reason_oem_themes
    "oem_games" -> R.string.reason_oem_games
    "oem_media" -> R.string.reason_oem_media
    "oem_community" -> R.string.reason_oem_community
    "oem_tips" -> R.string.reason_oem_tips
    "lockscreen_feed" -> R.string.reason_lockscreen_feed
    "ar" -> R.string.reason_ar
    "kids" -> R.string.reason_kids
    "cloud" -> R.string.reason_cloud
    "assistant" -> R.string.reason_assistant
    "share" -> R.string.reason_share
    "optional_google" -> R.string.reason_optional_google
    "optional_tool" -> R.string.reason_optional_tool
    "telemetry" -> R.string.reason_telemetry
    "keyboard" -> R.string.reason_keyboard
    "find_device" -> R.string.reason_find_device
    "account_service" -> R.string.reason_account_service
    "payment" -> R.string.reason_payment
    "unsafe" -> R.string.reason_unsafe
    else -> throw IllegalArgumentException("Unknown reason key: $key")
}
