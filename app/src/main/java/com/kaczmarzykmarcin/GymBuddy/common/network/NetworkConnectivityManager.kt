package com.kaczmarzykmarcin.GymBuddy.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menedżer do zarządzania stanem połączenia z internetem.
 */
@Singleton
class NetworkConnectivityManager @Inject constructor(
    private val context: Context
) {
    private val TAG = "NetworkConnectivity"

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    init {
        // Ustaw początkowy stan połączenia
        checkNetworkConnectivity()

        // Nasłuchuj zmian w połączeniu
        registerNetworkCallback()
    }

    /**
     * Sprawdza aktualny stan połączenia z internetem.
     */
    fun checkNetworkConnectivity() {
        val network = connectivityManager.activeNetwork
        val isConnected = hasNetworkCapabilities(network)
        _isNetworkAvailable.value = isConnected
        Log.d(TAG, "Network connectivity: ${if (isConnected) "Available" else "Unavailable"}")
    }

    /**
     * Sprawdza, czy urządzenie ma połączenie z internetem
     */
    fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        return hasNetworkCapabilities(network)
    }

    /**
     * Rejestruje callback do monitorowania zmian w połączeniu.
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            // Połączenie zostało nawiązane
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val hasInternet = hasNetworkCapabilities(network)
                _isNetworkAvailable.value = hasInternet
                Log.d(TAG, "Network became available: $hasInternet")
            }

            // Połączenie zostało utracone
            override fun onLost(network: Network) {
                super.onLost(network)
                _isNetworkAvailable.value = false
                Log.d(TAG, "Network lost")
            }

            // Zmieniły się możliwości sieci
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isNetworkAvailable.value = hasInternet
                Log.d(TAG, "Network capabilities changed: $hasInternet")
            }
        })
    }

    /**
     * Sprawdza, czy sieć ma wymagane możliwości.
     */
    private fun hasNetworkCapabilities(network: Network?): Boolean {
        if (network == null) return false

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}