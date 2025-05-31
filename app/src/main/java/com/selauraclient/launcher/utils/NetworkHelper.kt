package com.selauraclient.launcher.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData

class NetworkHelper(context: Context) : LiveData<Boolean>() {
private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        postValue(true)
    }

    override fun onLost(network: Network) {
        postValue(false)
    }
}

override fun onActive() {
    super.onActive()
    postValue(checkConnection())
    connectivityManager.registerDefaultNetworkCallback(networkCallback)
}

override fun onInactive() {
    super.onInactive()
    connectivityManager.unregisterNetworkCallback(networkCallback)
}

fun checkConnection(): Boolean {
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
}
