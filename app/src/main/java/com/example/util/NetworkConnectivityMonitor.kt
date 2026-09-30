package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NetworkState {
    object Connected : NetworkState()
    object Disconnected : NetworkState()
    object JustRestored : NetworkState()
}

class NetworkConnectivityMonitor private constructor(private val context: Context) {
    companion object {
        private const val TAG = "NetworkMonitor"
        @Volatile
        private var instance: NetworkConnectivityMonitor? = null

        fun getInstance(context: Context): NetworkConnectivityMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityMonitor(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkState = MutableStateFlow<NetworkState>(
        if (_isOnline.value) NetworkState.Connected else NetworkState.Disconnected
    )
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private var hasBeenOffline = false
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.w(TAG, "checkInitialConnectivity error: ${e.message}")
            true
        }
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) return

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch {
                            val wasOffline = hasBeenOffline
                            _isOnline.value = true
                            if (wasOffline) {
                                hasBeenOffline = false
                                _networkState.value = NetworkState.JustRestored
                                Log.i(TAG, "Network restored. Triggering automatic background sync...")
                                try {
                                    SyncManager.getInstance(context).onAppForeground(source = "network_restored")
                                } catch (e: Throwable) {
                                    Log.w(TAG, "SyncManager on network restored warning: ${e.message}")
                                }
                            } else {
                                _networkState.value = NetworkState.Connected
                            }
                        }
                    }

                    override fun onLost(network: Network) {
                        scope.launch {
                            hasBeenOffline = true
                            _isOnline.value = false
                            _networkState.value = NetworkState.Disconnected
                            Log.w(TAG, "Network connection lost. Activating offline Room cache...")
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        val isOnlineNow = hasInternet && isValidated
                        if (_isOnline.value != isOnlineNow) {
                            scope.launch {
                                _isOnline.value = isOnlineNow
                                if (!isOnlineNow) {
                                    hasBeenOffline = true
                                    _networkState.value = NetworkState.Disconnected
                                }
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    fun acknowledgeRestored() {
        if (_networkState.value is NetworkState.JustRestored) {
            _networkState.value = NetworkState.Connected
        }
    }
}
