package com.example.vpn.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.vpn.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkType = MutableStateFlow("Detecting...")
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startMonitoring(onNetworkLostOrChanged: (isLost: Boolean) -> Unit) {
        stopMonitoring()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkType()
                VpnController.getInstance(context).log(
                    LogLevel.INFO,
                    "NETWORK",
                    "Network interface available: ${_networkType.value}"
                )
                onNetworkLostOrChanged(false)
            }

            override fun onLost(network: Network) {
                updateNetworkType()
                VpnController.getInstance(context).log(
                    LogLevel.WARN,
                    "NETWORK",
                    "Active network interface lost. Triggering transition recovery."
                )
                onNetworkLostOrChanged(true)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateNetworkType()
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            updateNetworkType()
        } catch (e: Exception) {
            _networkType.value = "Unknown"
        }
    }

    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        networkCallback = null
    }

    private fun updateNetworkType() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val type = when {
            caps == null -> "No Internet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile (5G/LTE)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Active"
            else -> "Connected"
        }
        _networkType.value = type
    }
}
