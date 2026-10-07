package it.bbnss.moneta.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

data class NetworkStatus(val connected: Boolean, val wifi: Boolean) {
    companion object {
        fun read(context: Context): NetworkStatus {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            return NetworkStatus(
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
            )
        }
    }
}
