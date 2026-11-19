package com.a2heng.androc;

import android.content.Context;
import android.util.Log;

import com.a2heng.androc.contracts.PluginInfo;
import com.a2heng.androc.contracts.PluginManager;
import com.a2heng.androc.ui.data.AutomationBridge;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Loopback automation API (developer tool).
 *
 * GET  /state                    -> {ui, connected, serverUrl, local, saved}
 * POST /connect?url=&password=   -> connect/remember a server
 * POST /start-local              -> start the installed component and connect
 *
 * Bound to 127.0.0.1:42100, token header X-Androc-Token: androc-local-dev.
 * This is a development surface; it is not exposed on the LAN.
 */
public final class AutomationServer {
    private static final class AutomationServerHolder {
        private static Context context;

        static void set(Context ctx) {
            context = ctx;
        }

        static Context context() {
            return context;
        }
    }

    private static final String TAG = "AutomationServer";
    private static final int PORT = 42100;
    private static final String TOKEN = "androc-local-dev";
    private static volatile boolean started;

    private AutomationServer() {
    }

    public static synchronized void start(Context ctx) {
        if (ctx != null) AutomationServerHolder.set(ctx.getApplicationContext());
        if (started) return;
        started = true;
        Thread thread = new Thread(() -> loop(), "automation-api");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop() {
        try (ServerSocket server = new ServerSocket(PORT, 8,
                InetAddress.getByName("127.0.0.1"))) {
            Log.i(TAG, "automation API on 127.0.0.1:" + PORT);
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handle(socket), "automation-conn").start();
            }
        } catch (Exception e) {
            Log.w(TAG, "server failed", e);
            started = false;
        }
    }

    private static void handle(Socket socket) {
        try (socket;
             InputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
            String request = readLine(in);
            if (request == null) return;
            String[] parts = request.split(" ");
            String method = parts.length > 0 ? parts[0].toUpperCase(Locale.US) : "";
            String target = parts.length > 1 ? parts[1] : "/";
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int i = line.indexOf(':');
                if (i > 0) headers.put(line.substring(0, i).trim().toLowerCase(Locale.US),
                        line.substring(i + 1).trim());
            }
            String token = headers.get("x-androc-token");
            if (!TOKEN.equals(token)) {
                respond(out, 401, "{\"error\":\"token\"}");
                return;
            }
            int contentLength = 0;
            try {
                contentLength = Integer.parseInt(headers.getOrDefault("content-length", "0"));
            } catch (Exception ignored) {
            }
            byte[] bodyBytes = new byte[Math.min(contentLength, 1 << 20)];
            int bodyRead = 0;
            while (bodyRead < bodyBytes.length) {
                int n = in.read(bodyBytes, bodyRead, bodyBytes.length - bodyRead);
                if (n <= 0) break;
                bodyRead += n;
            }
            String requestBody = new String(bodyBytes, 0, bodyRead, StandardCharsets.UTF_8);
            int q = target.indexOf('?');
            String path = q < 0 ? target : target.substring(0, q);
            String query = q < 0 ? "" : target.substring(q + 1);
            Map<String, String> params = parseQuery(query);

            String body;
            if ("/state".equals(path)) {
                body = AutomationBridge.INSTANCE.state();
            } else if ("/connect".equals(path) && "POST".equals(method)) {
                body = AutomationBridge.INSTANCE.connect(
                        params.getOrDefault("url", ""),
                        params.getOrDefault("password", ""));
            } else if ("/start-local".equals(path) && "POST".equals(method)) {
                body = AutomationBridge.INSTANCE.startLocal();
            } else if ("/ping".equals(path)) {
                body = "{\"ok\":true}";
            } else if ("/logs".equals(path)) {
                body = new org.json.JSONObject()
                        .put("log", ThinRuntime.logTail(AutomationServerHolder.context()))
                        .toString();
            } else if ("/proxy".equals(path)) {
                body = proxy(params.getOrDefault("path", "/api/info"), method, requestBody);
            } else if ("/market".equals(path)) {
                if ("POST".equals(method)) {
                    MarketConfig.setIndexUrl(AutomationServerHolder.context(),
                            params.getOrDefault("url", ""));
                }
                body = new org.json.JSONObject()
                        .put("url", MarketConfig.indexUrl(AutomationServerHolder.context()))
                        .toString();
            } else if ("/plugins".equals(path)) {
                body = pluginsJson();
            } else if ("/plugin/install".equals(path) && "POST".equals(method)) {
                body = pluginAction(params.getOrDefault("id", ""), true);
            } else if ("/plugin/uninstall".equals(path) && "POST".equals(method)) {
                body = pluginAction(params.getOrDefault("id", ""), false);
            } else if ("/local".equals(path)) {
                body = new org.json.JSONObject()
                        .put("installed", ThinRuntime.installed(AutomationServerHolder.context()))
                        .put("version", ThinRuntime.installedVersion(AutomationServerHolder.context()))
                        .put("running", ThinRuntime.running())
                        .put("url", "http://127.0.0.1:" + ThinRuntime.port(AutomationServerHolder.context()))
                        .toString();
            } else {
                respond(out, 404, "{\"error\":\"not found\"}");
                return;
            }
            respond(out, 200, body);
        } catch (Exception e) {
            Log.w(TAG, "request failed", e);
        }
    }

    /** GET /plugins — debug-only view of the plugin layer. */
    private static String pluginsJson() throws Exception {
        AppPluginManager manager = new AppPluginManager(AutomationServerHolder.context());
        org.json.JSONArray arr = new org.json.JSONArray();
        for (PluginInfo p : manager.list()) {
            arr.put(new org.json.JSONObject()
                    .put("id", p.id())
                    .put("name", p.name())
                    .put("installed", p.installed())
                    .put("version", p.version())
                    .put("latest", p.latestVersion())
                    .put("bundled", p.bundled())
                    .put("skills", new org.json.JSONArray(p.skills())));
        }
        return new org.json.JSONObject().put("plugins", arr).toString();
    }

    /** POST /plugin/install|uninstall?id= */
    private static String pluginAction(String id, boolean install) throws Exception {
        if (id == null || id.isEmpty()) {
            return new org.json.JSONObject().put("ok", false).put("message", "missing id").toString();
        }
        AppPluginManager manager = new AppPluginManager(AutomationServerHolder.context());
        CountDownLatch latch = new CountDownLatch(1);
        String[] message = {""};
        boolean[] ok = {false};
        PluginManager.Callback callback = (pid, success, msg) -> {
            ok[0] = success;
            message[0] = msg;
            latch.countDown();
        };
        if (install) manager.install(id, callback);
        else manager.uninstall(id, callback);
        if (!latch.await(240, TimeUnit.SECONDS)) {
            return new org.json.JSONObject().put("ok", false).put("message", "timeout").toString();
        }
        return new org.json.JSONObject().put("ok", ok[0]).put("message", message[0]).toString();
    }

    private static String proxy(String apiPath, String method, String requestBody) throws Exception {
        Context ctx = AutomationServerHolder.context();
        int port = ThinRuntime.port(ctx);
        String url = "http://127.0.0.1:" + port + (apiPath.startsWith("/") ? apiPath : "/" + apiPath);
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setConnectTimeout(2000);
        c.setReadTimeout(15000);
        c.setUseCaches(false);
        String auth = android.util.Base64.encodeToString(
                ("opencode:" + LocalRuntime.serverPassword(ctx)).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                android.util.Base64.NO_WRAP);
        c.setRequestProperty("Authorization", "Basic " + auth);
        c.setRequestProperty("Accept", "application/json");
        if ("POST".equals(method)) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            byte[] payload = requestBody.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(payload.length);
            c.getOutputStream().write(payload);
            c.getOutputStream().close();
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        if (in != null) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
        }
        c.disconnect();
        if (code >= 400) {
            return new org.json.JSONObject().put("status", code)
                    .put("body", out.toString("UTF-8")).toString();
        }
        return out.toString("UTF-8");
    }

    private static Map<String, String> parseQuery(String query) throws Exception {
        Map<String, String> out = new HashMap<>();
        if (query == null || query.isEmpty()) return out;
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            out.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
        }
        return out;
    }

    private static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
            if (sb.length() > 8192) break;
        }
        return c < 0 && sb.length() == 0 ? null : sb.toString();
    }

    private static void respond(OutputStream out, int code, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + code + " " + (code == 200 ? "OK" : "Error") + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }
}
