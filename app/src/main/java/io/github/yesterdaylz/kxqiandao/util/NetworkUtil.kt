package io.github.yesterdaylz.kxqiandao.util

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/** 当前网络环境判断 */
object NetworkUtil {

    enum class NetState {
        /** 在科协网内 */
        Campus,

        /** 有网，但不在科协网 */
        Other,

        /** 没有可用网络 */
        Offline,
    }

    fun currentState(context: Context): NetState {
        if (!hasNetwork(context)) return NetState.Offline
        return if (isOnCampus()) NetState.Campus else NetState.Other
    }

    /** 本机 IPv4 是否落在科协网段 */
    fun isOnCampus(): Boolean = localIpv4().any { ip ->
        Constants.CAMPUS_LAN_PREFIXES.any { ip.startsWith(it) }
    }

    private fun hasNetwork(context: Context): Boolean =
        context.getSystemService(ConnectivityManager::class.java)?.activeNetwork != null

    private fun localIpv4(): List<String> = runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .flatMap { Collections.list(it.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
    }.getOrDefault(emptyList())
}
