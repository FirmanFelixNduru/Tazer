package com.tazer.taskorganizer.integration

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal fun networkAvailability(context: Context) = callbackFlow {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    fun reachable(capabilities: NetworkCapabilities?) =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    trySend(reachable(manager.getNetworkCapabilities(manager.activeNetwork)))
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(reachable(capabilities))
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }
    manager.registerDefaultNetworkCallback(callback)
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
