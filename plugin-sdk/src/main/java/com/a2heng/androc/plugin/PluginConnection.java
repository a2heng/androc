package com.a2heng.androc.plugin;

import com.a2heng.androc.contracts.RuntimeConnection;

import org.json.JSONObject;

/**
 * Parse the JSON returned by {@link IRuntimePlugin#start()} / connection().
 *
 * <pre>{"url":"http://127.0.0.1:4096","username":"opencode","password":"…","running":true}</pre>
 */
public final class PluginConnection {
    public final String url;
    public final String username;
    public final String password;
    public final boolean running;
    public final String message;

    private PluginConnection(String url, String username, String password,
                             boolean running, String message) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.running = running;
        this.message = message;
    }

    public static PluginConnection parse(String raw) {
        try {
            JSONObject o = new JSONObject(raw == null ? "{}" : raw);
            String url = o.optString("url", "");
            return new PluginConnection(
                    url,
                    o.optString("username", RuntimeConnection.DEFAULT_USERNAME),
                    o.optString("password", ""),
                    o.optBoolean("running", false),
                    o.optString("message", ""));
        } catch (Exception e) {
            return new PluginConnection("", RuntimeConnection.DEFAULT_USERNAME,
                    "", false, e.getMessage());
        }
    }

    public RuntimeConnection toRuntimeConnection() {
        return new RuntimeConnection(url, username, password);
    }
}
