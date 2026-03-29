package com.app.lumen.services

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    // Defaults
    private val defaults = mapOf(
        "android_review_enabled" to true,
        "android_weekly_split_enabled" to true,
    )

    val reviewEnabled: Boolean
        get() = remoteConfig.getBoolean("android_review_enabled")

    val weeklySplitEnabled: Boolean
        get() = remoteConfig.getBoolean("android_weekly_split_enabled")

    fun initialize() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
    }
}
