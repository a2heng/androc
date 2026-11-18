package com.a2heng.androc.ui.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug/automation bridge for the running UI.
 *
 * The UI owns AppModel; a loopback HTTP server (app module) calls this to read
 * state and trigger actions without adb taps. Deliberately small: semantic
 * actions, not pixel coordinates.
 */
object AutomationBridge {
    @Volatile
    var model: AppModel? = null

    @Synchronized
    fun state(): String {
        val m = model ?: return JSONObject().put("ui", false).toString()
        val launcher = LocalRuntimeHost.launcher
        return JSONObject().apply {
            put("ui", true)
            put("connected", m.connected)
            put("serverUrl", m.serverUrl)
            put("connecting", m.connecting)
            put("error", m.connectionError ?: "")
            put("local", JSONObject().apply {
                put("available", launcher != null)
                put("installed", launcher?.installed == true)
                put("version", launcher?.version ?: "")
                put("running", launcher?.installed == true)
            })
            put("saved", JSONArray().apply {
                m.savedConnections.forEach { c ->
                    put(JSONObject().apply {
                        put("url", c.url)
                        put("password", if (c.password.isBlank()) "" else "***")
                    })
                }
            })
        }.toString()
    }

    @Synchronized
    fun connect(url: String, password: String): String {
        val m = model ?: return error("ui not ready")
        m.connect(url, password, remember = true)
        return JSONObject().put("ok", true).toString()
    }

    @Synchronized
    fun startLocal(): String {
        val m = model ?: return error("ui not ready")
        val launcher = LocalRuntimeHost.launcher ?: return error("local launcher missing")
        if (!launcher.installed) return error("local component not installed")
        var result = error("pending")
        val lock = Object()
        launcher.start(
            { url, password ->
                m.connect(url, password, remember = true)
                synchronized(lock) {
                    result = JSONObject().put("ok", true).put("url", url).toString()
                    lock.notifyAll()
                }
            },
            { message ->
                synchronized(lock) {
                    result = error(message)
                    lock.notifyAll()
                }
            },
        )
        synchronized(lock) {
            lock.wait(120_000)
        }
        return result.toString()
    }

    private fun error(message: String): String =
        JSONObject().put("ok", false).put("error", message).toString()
}
