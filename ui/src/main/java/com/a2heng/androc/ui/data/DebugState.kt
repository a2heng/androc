package com.a2heng.androc.ui.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 调试快照：给 app 模块的本机调试 HTTP 服务用。
 *
 * 只暴露只读状态，不携带密码；服务监听 127.0.0.1，外部通过
 * `adb forward tcp:42010 tcp:42010` 访问。
 */
object DebugState {
    @Volatile
    var model: AppModel? = null
        private set

    fun attach(m: AppModel) {
        model = m
    }

    fun snapshot(pretty: Boolean = true): String {
        val m = model ?: return JSONObject().put("model", JSONObject.NULL).toString()
        val root = JSONObject()
        root.put("serverUrl", m.serverUrl)
        root.put("connected", m.connected)
        root.put("connecting", m.connecting)
        root.put("connectionError", m.connectionError ?: JSONObject.NULL)
        root.put("localGitActive", m.localGitActive)
        root.put("detailLevel", m.detailLevel)
        m.current?.let { cur ->
            root.put(
                "session",
                JSONObject()
                    .put("id", cur.id)
                    .put("title", cur.shortTitle)
                    .put("directory", cur.directory),
            )
        }
        root.put(
            "git",
            JSONObject()
                .put("directory", m.gitDirectory ?: JSONObject.NULL)
                .put("provider", m.gitInfo.provider)
                .put("branch", m.gitInfo.branch.current)
                .put("mode", m.gitMode)
                .put("loading", m.gitLoading)
                .put("error", m.gitError ?: JSONObject.NULL)
                .put("diagnostic", m.gitDiagnostic ?: JSONObject.NULL)
                .put(
                    "status",
                    JSONArray().apply {
                        m.gitStatus.forEach { s ->
                            put(
                                JSONObject()
                                    .put("file", s.file)
                                    .put("status", s.status)
                                    .put("add", s.additions)
                                    .put("del", s.deletions),
                            )
                        }
                    },
                )
                .put(
                    "diffFiles",
                    JSONArray().apply {
                        m.gitDiffFiles.forEach { d ->
                            put(
                                JSONObject()
                                    .put("file", d.file)
                                    .put("status", d.status)
                                    .put("add", d.additions)
                                    .put("del", d.deletions)
                                    .put("patchLen", d.patch.length),
                            )
                        }
                    },
                ),
        )
        root.put(
            "shells",
            JSONArray().apply {
                m.shells.forEach { s ->
                    put(
                        JSONObject()
                            .put("id", s.id)
                            .put("command", s.command)
                            .put("status", s.status)
                            .put("sessionID", s.sessionID)
                            .put("started", s.started),
                    )
                }
            },
        )
        root.put(
            "sessions",
            JSONArray().apply {
                m.sessions.take(20).forEach { s ->
                    put(
                        JSONObject()
                            .put("id", s.id)
                            .put("title", s.shortTitle)
                            .put("directory", s.directory)
                            .put("updated", s.updated),
                    )
                }
            },
        )
        root.put(
            "messages",
            JSONArray().apply {
                m.messages.takeLast(40).forEach { msg ->
                    put(
                        JSONObject()
                            .put("id", msg.id)
                            .put("role", msg.role)
                            .put("created", msg.created)
                            .put("text", msg.text.take(600))
                            .put("error", msg.error ?: JSONObject.NULL)
                            .put(
                                "parts",
                                JSONArray().apply {
                                    msg.parts.forEach { part ->
                                        val o = JSONObject()
                                        when (part) {
                                            is Part.Text -> {
                                                o.put("type", "text")
                                                o.put("text", part.text.take(600))
                                            }
                                            is Part.Reasoning -> {
                                                o.put("type", "reasoning")
                                                o.put("text", part.text.take(600))
                                            }
                                            is Part.Tool -> {
                                                o.put("type", "tool")
                                                o.put("name", part.name)
                                                o.put("toolID", part.toolID)
                                                o.put("status", part.status)
                                                o.put("input", part.input.take(300))
                                                o.put("output", part.output.take(300))
                                                o.put("error", part.error)
                                            }
                                        }
                                        put(o)
                                    }
                                },
                            ),
                    )
                }
            },
        )
        return if (pretty) root.toString(2) else root.toString()
    }
}
