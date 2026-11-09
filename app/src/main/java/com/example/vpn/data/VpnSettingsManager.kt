package com.example.vpn.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VpnSettings(
    val autoConnectOnLaunch: Boolean = false,
    val autoReconnectOnNetworkLoss: Boolean = true,
    val selectedDnsPreset: String = "1.1.1.1 (Cloudflare)",
    val customDns: String = "1.1.1.1",
    val defaultMtu: Int = 1400,
    val connectionTimeoutSeconds: Int = 30,
    val themeMode: String = "SYSTEM",
    val loggingLevel: String = "DEBUG",
    val natKeepaliveInterval: Int = 20, // seconds
    val activeProfileId: String? = null
)

class VpnSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aegis_vpn_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<VpnSettings> = _settings.asStateFlow()

    private fun loadSettings(): VpnSettings {
        return VpnSettings(
            autoConnectOnLaunch = prefs.getBoolean("auto_connect", false),
            autoReconnectOnNetworkLoss = prefs.getBoolean("auto_reconnect", true),
            selectedDnsPreset = prefs.getString("dns_preset", "1.1.1.1 (Cloudflare)") ?: "1.1.1.1 (Cloudflare)",
            customDns = prefs.getString("custom_dns", "1.1.1.1") ?: "1.1.1.1",
            defaultMtu = prefs.getInt("default_mtu", 1400),
            connectionTimeoutSeconds = prefs.getInt("conn_timeout", 30),
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            loggingLevel = prefs.getString("logging_level", "DEBUG") ?: "DEBUG",
            natKeepaliveInterval = prefs.getInt("nat_keepalive", 20),
            activeProfileId = prefs.getString("active_profile_id", null)
        )
    }

    fun updateAutoConnect(enabled: Boolean) {
        prefs.edit().putBoolean("auto_connect", enabled).apply()
        _settings.value = _settings.value.copy(autoConnectOnLaunch = enabled)
    }

    fun updateAutoReconnect(enabled: Boolean) {
        prefs.edit().putBoolean("auto_reconnect", enabled).apply()
        _settings.value = _settings.value.copy(autoReconnectOnNetworkLoss = enabled)
    }

    fun updateDns(preset: String, dnsIp: String) {
        prefs.edit()
            .putString("dns_preset", preset)
            .putString("custom_dns", dnsIp)
            .apply()
        _settings.value = _settings.value.copy(selectedDnsPreset = preset, customDns = dnsIp)
    }

    fun updateMtu(mtu: Int) {
        val safeMtu = mtu.coerceIn(1280, 1500)
        prefs.edit().putInt("default_mtu", safeMtu).apply()
        _settings.value = _settings.value.copy(defaultMtu = safeMtu)
    }

    fun updateTimeout(timeoutSeconds: Int) {
        val safeTimeout = timeoutSeconds.coerceIn(10, 120)
        prefs.edit().putInt("conn_timeout", safeTimeout).apply()
        _settings.value = _settings.value.copy(connectionTimeoutSeconds = safeTimeout)
    }

    fun updateTheme(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
        _settings.value = _settings.value.copy(themeMode = theme)
    }

    fun updateLoggingLevel(level: String) {
        prefs.edit().putString("logging_level", level).apply()
        _settings.value = _settings.value.copy(loggingLevel = level)
    }

    fun updateActiveProfileId(profileId: String?) {
        prefs.edit().putString("active_profile_id", profileId).apply()
        _settings.value = _settings.value.copy(activeProfileId = profileId)
    }
}
