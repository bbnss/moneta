package it.bbnss.moneta.core.model

import java.time.Duration
import java.net.URI

enum class RequestKind { AUTOMATIC, MANUAL, HISTORY, VERIFY }
enum class NetworkBlock { OFFLINE, WIFI, DISCONNECTED, MANUAL_ONLY, NOT_DUE }

object UpdatePolicy {
    fun block(kind: RequestKind, offline: Boolean, wifiOnly: Boolean, intervalHours: Int,
              connected: Boolean, wifi: Boolean, verifiedAge: Duration?): NetworkBlock? = when {
        offline -> NetworkBlock.OFFLINE
        !connected -> NetworkBlock.DISCONNECTED
        wifiOnly && !wifi -> NetworkBlock.WIFI
        kind == RequestKind.AUTOMATIC && intervalHours <= 0 -> NetworkBlock.MANUAL_ONLY
        kind == RequestKind.AUTOMATIC && verifiedAge != null && verifiedAge < Duration.ofHours(intervalHours.toLong()) -> NetworkBlock.NOT_DUE
        else -> null
    }
}

object CustomEndpoint {
    /** HTTPS origin plus optional deployment path, without query, fragment or credentials. */
    fun normalize(raw: String): String? = runCatching {
        val uri = URI(raw.trim())
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank())
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
        require(uri.port == -1 || uri.port in 1..65535)
        uri.toASCIIString().trimEnd('/')
    }.getOrNull()
}
