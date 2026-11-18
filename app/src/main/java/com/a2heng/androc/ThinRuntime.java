package com.a2heng.androc;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Thin runtime shell: starts an installed OpenCode pack with the musl loader.
 *
 * <p>No proot, no Termux prefix. The loader is in nativeLibraryDir (the only
 * executable place), the binary lives in files/runtime/&lt;version&gt;/opencode,
 * and the UI receives the same URL+password pair a remote server would use.</p>
 */
public final class ThinRuntime {
    private static final String TAG = "ThinRuntime";
    private static Process process;

    private ThinRuntime() {
    }

    public static File installedBinary(Context ctx) {
        File[] dirs = new File(ctx.getFilesDir(), "runtime").listFiles();
        if (dirs == null) return null;
        File best = null;
        String bestVersion = "";
        for (File dir : dirs) {
            if (!dir.isDirectory()) continue;
            File bin = new File(dir, "opencode");
            if (!bin.isFile() || bin.length() < 10L * 1024 * 1024) continue;
            String v = dir.getName();
            if (best == null || compareVersion(v, bestVersion) > 0) {
                best = bin;
                bestVersion = v;
            }
        }
        return best;
    }

    public static boolean installed(Context ctx) {
        return installedBinary(ctx) != null;
    }

    public static String installedVersion(Context ctx) {
        File bin = installedBinary(ctx);
        return bin == null || bin.getParentFile() == null ? null : bin.getParentFile().getName();
    }

    public static synchronized boolean running() {
        return process != null && process.isAlive();
    }

    public static synchronized boolean start(Context ctx) throws IOException {
        if (running()) return true;
        Context app = ctx.getApplicationContext();
        File bin = installedBinary(app);
        if (bin == null) throw new IOException("OpenCode 组件未安装");
        killStale(app);

        File nd = new File(app.getApplicationInfo().nativeLibraryDir);
        File musl = new File(nd, "libld_musl.so");
        if (!musl.isFile()) throw new IOException("缺少 musl loader");

        File home = app.getFilesDir();
        File lib = new File(home, "lib");
        File config = new File(home, ".config/opencode");
        File tmp = new File(app.getCacheDir(), "tmp");
        File logs = new File(home, "logs");
        for (File d : new File[]{lib, config, tmp, logs}) {
            if (!d.isDirectory() && !d.mkdirs()) throw new IOException("无法创建 " + d);
        }
        copy(new File(nd, "libstdcxx.so"), new File(lib, "libstdc++.so.6"));
        copy(new File(nd, "libgcc_s.so"), new File(lib, "libgcc_s.so.1"));

        String password = LocalRuntime.serverPassword(app);
        int port = port(app);
        String libPath = lib.getAbsolutePath() + ":" + nd.getAbsolutePath();
        File log = new File(logs, "thin-runtime.log");
        // Do not carry the previous failure's text into the dialog.
        try (FileOutputStream truncate = new FileOutputStream(log)) {
            // truncate
        }
        List<String> cmd = Arrays.asList(
                musl.getAbsolutePath(),
                "--library-path", libPath,
                bin.getAbsolutePath(),
                "serve",
                "--hostname", "127.0.0.1",
                "--port", Integer.toString(port),
                "--print-logs",
                "--log-level", "info");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(home);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        Map<String, String> env = pb.environment();
        env.clear();
        env.put("HOME", home.getAbsolutePath());
        env.put("PWD", home.getAbsolutePath());
        env.put("TMPDIR", tmp.getAbsolutePath());
        env.put("PATH", "/system/bin");
        env.put("LANG", "C.UTF-8");
        env.put("OPENCODE_CONFIG_DIR", config.getAbsolutePath());
        env.put("OPENCODE_DISABLE_AUTOUPDATE", "1");
        env.put("OPENCODE_SERVER_USERNAME", "opencode");
        env.put("OPENCODE_SERVER_PASSWORD", password);
        env.put("OPENCODE_PASSWORD", password);
        Log.i(TAG, "starting " + bin + " v" + installedVersion(app));

        process = pb.start();
        return true;
    }

    public static synchronized void stop() {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            if (process.isAlive()) process.destroyForcibly();
            process = null;
        }
    }

    /** Blocking health wait; call from a worker thread. */
    public static boolean awaitReady(Context ctx, long timeoutMs) {
        String password = LocalRuntime.serverPassword(ctx.getApplicationContext());
        int port = port(ctx.getApplicationContext());
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!running()) return false;
            if (healthy(port, password)) return true;
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static String logTail(Context ctx) {
        File log = new File(ctx.getFilesDir(), "logs/thin-runtime.log");
        if (!log.isFile()) return "";
        try (InputStream in = new FileInputStream(log)) {
            byte[] all = new byte[(int) Math.min(log.length(), 8192)];
            int off = 0;
            while (off < all.length) {
                int n = in.read(all, off, all.length - off);
                if (n < 0) break;
                off += n;
            }
            String text = new String(all, 0, off, StandardCharsets.UTF_8);
            return text.length() > 1200 ? text.substring(text.length() - 1200) : text;
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean healthy(int port, String password) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/api/info").openConnection();
            c.setConnectTimeout(1200);
            c.setReadTimeout(2000);
            c.setUseCaches(false);
            String auth = Base64.encodeToString(
                    ("opencode:" + password).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            c.setRequestProperty("Authorization", "Basic " + auth);
            return c.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    public static int port(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("thin_runtime", Context.MODE_PRIVATE);
        int saved = sp.getInt("port", 0);
        if (saved > 0) return saved;
        int picked = pickFreePort();
        sp.edit().putInt("port", picked).apply();
        return picked;
    }

    private static int pickFreePort() {
        for (int p = 4098; p < 4200; p++) {
            try (ServerSocket socket = new ServerSocket(p)) {
                return p;
            } catch (Exception ignored) {
            }
        }
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 4098;
        }
    }

    /** Kill a previous opencode process left by an older APK/install. */
    private static void killStale(Context ctx) {
        String needle = new File(ctx.getFilesDir(), "runtime").getAbsolutePath();
        File[] procs = new File("/proc").listFiles();
        if (procs == null) return;
        for (File dir : procs) {
            if (!dir.isDirectory() || !dir.getName().matches("\\d+")) continue;
            int pid;
            try {
                pid = Integer.parseInt(dir.getName());
            } catch (Exception e) {
                continue;
            }
            if (pid == android.os.Process.myPid()) continue;
            File cmdline = new File(dir, "cmdline");
            try (InputStream in = new FileInputStream(cmdline)) {
                byte[] buf = new byte[512];
                int n = in.read(buf);
                if (n <= 0) continue;
                String cmd = new String(buf, 0, n, StandardCharsets.UTF_8);
                if (cmd.contains(needle) && cmd.contains("opencode")) {
                    android.os.Process.killProcess(pid);
                    Log.i(TAG, "killed stale " + pid);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void copy(File src, File dst) throws IOException {
        if (!src.isFile()) throw new IOException("缺少 " + src.getName());
        if (dst.isFile() && dst.length() == src.length()) return;
        try (InputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static int compareVersion(String a, String b) {
        String[] x = a.split("[.-]");
        String[] y = b.split("[.-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? parseInt(x[i]) : 0;
            int yi = i < y.length ? parseInt(y[i]) : 0;
            if (xi != yi) return Integer.compare(xi, yi);
        }
        return 0;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
