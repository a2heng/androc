package com.a2heng.androc;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 本机调试 HTTP 服务：只监听 127.0.0.1:42010，供 adb forward 后查询。
 *
 * <pre>
 * adb forward tcp:42010 tcp:42010
 * curl http://127.0.0.1:42010/state
 * curl -G http://127.0.0.1:42010/git --data-urlencode dir=/path \
 *      --data-urlencode 'args=log --oneline -5'
 * curl -G http://127.0.0.1:42010/shell --data-urlencode 'cmd=git status --short'
 * </pre>
 *
 * 不暴露密码；只读状态 + 在 proot 里执行命令，方便开发期排查 UI 看不到的内容。
 */
public final class DebugApi {
    private static final int PORT = 42010;
    private static ServerSocket server;
    private static Thread thread;

    private DebugApi() {
    }

    public static synchronized void start(Context ctx) {
        if (thread != null && thread.isAlive()) return;
        final Context app = ctx.getApplicationContext();
        thread = new Thread(() -> {
            try {
                server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = server.accept();
                    Thread worker = new Thread(() -> handle(app, socket), "debug-api-client");
                    worker.setDaemon(true);
                    worker.start();
                }
            } catch (Exception ignored) {
                // 端口被占/进程退出都无所谓，调试服务不是必需品
            }
        }, "debug-api");
        thread.setDaemon(true);
        thread.start();
    }

    private static void handle(Context ctx, Socket socket) {
        try (Socket s = socket) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null) return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !"GET".equals(parts[0])) {
                respond(s, 405, error("GET only"));
                return;
            }
            String target = parts[1];
            int q = target.indexOf('?');
            String path = q >= 0 ? target.substring(0, q) : target;
            String query = q >= 0 ? target.substring(q + 1) : "";
            Map<String, String> params = parseQuery(query);

            switch (path) {
                case "/health":
                    respond(s, 200, "{\"ok\":true}");
                    return;
                case "/state":
                    respond(s, 200, com.a2heng.androc.ui.data.DebugState.INSTANCE.snapshot(true));
                    return;
                case "/git": {
                    String dir = params.get("dir");
                    String args = params.getOrDefault("args", "status --porcelain=v1 -z");
                    if (dir == null || dir.isEmpty()) {
                        respond(s, 400, error("dir required"));
                        return;
                    }
                    String command = "cd -- " + quote(dir) + " 2>/dev/null || exit 97; "
                            + "git -c safe.directory='*' " + args + " 2>&1; "
                            + "printf '\\n__RC__:%s\\n' $?";
                    respond(s, 200, output(LocalRuntime.runInSandbox(ctx, command, 30_000, 4_000_000)));
                    return;
                }
                case "/shell": {
                    String command = params.get("cmd");
                    if (command == null || command.isEmpty()) {
                        respond(s, 400, error("cmd required"));
                        return;
                    }
                    respond(s, 200, output(LocalRuntime.runInSandbox(ctx, command, 30_000, 4_000_000)));
                    return;
                }
                default:
                    respond(s, 404, error("not found"));
            }
        } catch (Exception ignored) {
        }
    }

    private static String output(String text) {
        try {
            return new JSONObject().put("output", text == null ? "" : text).toString();
        } catch (Exception e) {
            return "{\"output\":\"\"}";
        }
    }

    private static String error(String message) {
        try {
            return new JSONObject().put("error", message).toString();
        } catch (Exception e) {
            return "{\"error\":\"\"}";
        }
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> out = new HashMap<>();
        if (query == null || query.isEmpty()) return out;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            try {
                String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                String value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                out.put(key, value);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static void respond(Socket socket, int code, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            OutputStream raw = socket.getOutputStream();
            Writer writer = new OutputStreamWriter(raw, StandardCharsets.UTF_8);
            writer.write("HTTP/1.1 " + code + " " + (code == 200 ? "OK" : "Error") + "\r\n");
            writer.write("Content-Type: application/json; charset=utf-8\r\n");
            writer.write("Content-Length: " + bytes.length + "\r\n");
            writer.write("Connection: close\r\n\r\n");
            writer.flush();
            raw.write(bytes);
            raw.flush();
        } catch (Exception ignored) {
        }
    }
}
