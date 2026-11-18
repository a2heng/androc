package com.a2heng.androc.ui.data

/**
 * Host hook for the on-device OpenCode component (pack / runtime plugin).
 *
 * The UI only asks: is it installed, and can you start it for me? The host
 * owns proot, the server process and the password; it hands back the same
 * RuntimeConnection the remote path uses. Thin builds leave this null until
 * the market has installed the component.
 */
interface LocalRuntimeLauncher {
    val installed: Boolean
    val version: String?

    fun start(onReady: (url: String, password: String) -> Unit, onError: (String) -> Unit)

    fun stop() {}
}

object LocalRuntimeHost {
    @Volatile
    var launcher: LocalRuntimeLauncher? = null
}
