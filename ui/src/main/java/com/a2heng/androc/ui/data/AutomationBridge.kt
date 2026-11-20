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
            put("updateMessage", m.updateMessage)
            put("updateProgress", m.updateProgress)
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

    /** GET /update/check：检查自更新，返回选中的 APK（thin/bundle 必须一致）。 */
    @Synchronized
    fun updateCheck(): String {
        return try {
            val info = kotlinx.coroutines.runBlocking { UpdateClient.latest() }
            JSONObject().apply {
                put("ok", info != null)
                put("thin", AppBuild.thin)
                put("tag", info?.tag ?: "")
                put("apk", info?.apkUrl ?: "")
            }.toString()
        } catch (e: Exception) {
            error(e.message ?: "check failed")
        }
    }

    /** POST /update/download：下载当前发行版的 APK，并交给系统安装器。 */
    @Synchronized
    fun updateDownload(): String {
        val m = model ?: return error("ui not ready")
        if (m.updateInfo == null) {
            // 还没检查过：先发起检查，稍后再调一次本接口。
            m.checkUpdate()
            return JSONObject().put("ok", true).put("message", "正在检查更新").toString()
        }
        m.downloadUpdate()
        return JSONObject().put("ok", true).put("message", m.updateMessage).toString()
    }

    private fun error(message: String): String =
        JSONObject().put("ok", false).put("error", message).toString()
}
