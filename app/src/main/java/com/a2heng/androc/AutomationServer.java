package com.a2heng.androc;

import android.content.Context;
import android.util.Log;

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
    private static final String TAG = "AutomationServer";
    private static final int PORT = 42100;
    private static final String TOKEN = "androc-local-dev";
    private static volatile boolean started;

    private AutomationServer() {
    }

    public static synchronized void start(Context ctx) {
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
            } else {
                respond(out, 404, "{\"error\":\"not found\"}");
                return;
            }
            respond(out, 200, body);
        } catch (Exception e) {
            Log.w(TAG, "request failed", e);
        }
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
