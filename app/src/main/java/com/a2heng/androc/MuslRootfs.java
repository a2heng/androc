package com.a2heng.androc;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs commands inside the musl/Alpine rootfs shipped by the android pack.
 *
 * The rootfs is `files/packs/android/<version>/rootfs`. The app home is bound
 * at its real absolute path, so sessions/projects keep the same paths the UI
 * already uses.
 */
final class MuslRootfs {
    private static final String TAG = "OpenCodeMusl";

    private MuslRootfs() {
    }

    static File dir(Context ctx) {
        // 1) android 包：完整的 musl rootfs + JDK/SDK/Gradle，装了就优先用它，
        //    本机 OpenCode 和端侧构建共用这一套。
        File android = packRootfs(ctx, "android");
        if (android != null) return android;
        // 2) OC 组件自带的 runtime：files/runtime/<ver>/rootfs
        File runtime = new File(ctx.getFilesDir(), "runtime");
        File[] versions = runtime.listFiles(File::isDirectory);
        if (versions != null) {
            File best = null;
            for (File v : versions) {
                if (!new File(v, "rootfs").isDirectory()) continue;
                if (best == null || v.getName().compareTo(best.getName()) > 0) best = v;
            }
            if (best != null) return new File(best, "rootfs");
        }
        // 3) bundle 内置的 runtime
        File bundled = RuntimeAssets.rootfs(ctx);
        if (bundled != null) return bundled;
        // 4) 兼容：旧版 runtime-core 包
        return packRootfs(ctx, "runtime-core");
    }

    private static File packRootfs(Context ctx, String id) {
        File pack = PackInstaller.installedDir(ctx, id);
        if (pack == null) return null;
        File rootfs = new File(pack, "rootfs");
        return rootfs.isDirectory() ? rootfs : null;
    }

    static boolean available(Context ctx) {
        return dir(ctx) != null;
    }

    /** proot command that runs `args` inside the rootfs. */
    static List<String> command(Context ctx, List<String> args, String cwd) {
        File rootfs = dir(ctx);
        if (rootfs == null) return null;
        File home = LocalRuntime.homeDir(ctx);
        File nd = new File(ctx.getApplicationInfo().nativeLibraryDir);
        File proot = new File(nd, "libproot.so");
        File loader = new File(nd, "libproot_loader.so");
        File tmp = new File(ctx.getCacheDir(), "tmp/musl");
        if (!tmp.isDirectory() && !tmp.mkdirs()) return null;

        // proot bind targets must exist inside the rootfs; create the app home
        // path so `-b home:home` keeps absolute paths identical.
        String homePath = home.getAbsolutePath();
        File homeInRoot = new File(rootfs.getAbsolutePath() + homePath);
        if (!homeInRoot.isDirectory() && !homeInRoot.mkdirs()) {
            Log.w(TAG, "cannot create " + homeInRoot);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("/system/bin/sh");
        cmd.add("-c");
        cmd.add("LD_LIBRARY_PATH=" + q(nd.getAbsolutePath())
                + " PROOT_LOADER=" + q(loader.getAbsolutePath())
                + " PROOT_TMP_DIR=" + q(tmp.getAbsolutePath())
                + " exec \"$@\"");
        cmd.add("musl");
        cmd.add(proot.getAbsolutePath());
        cmd.add("--kill-on-exit");
        cmd.add("-r");
        cmd.add(rootfs.getAbsolutePath());
        bind(cmd, homePath, homePath);
        bind(cmd, "/dev", "/dev");
        bind(cmd, "/proc", "/proc");
        bind(cmd, "/sys", "/sys");
        bind(cmd, "/system", "/system");
        File etc = new File(home, ".oc-etc");
        File hosts = new File(etc, "hosts");
        if (hosts.isFile()) bind(cmd, hosts.getAbsolutePath(), "/etc/hosts");
        File resolv = new File(etc, "resolv.conf");
        if (resolv.isFile()) bind(cmd, resolv.getAbsolutePath(), "/etc/resolv.conf");
        cmd.add("-w");
        cmd.add(cwd == null ? homePath : cwd);
        cmd.addAll(args);
        return cmd;
    }

    private static void bind(List<String> cmd, String host, String guest) {
        cmd.add("-b");
        cmd.add(host + ":" + guest);
    }

    private static String q(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    /** Debug helper: run a shell command and return stdout+stderr. */
    static String run(Context ctx, String command) {
        if (dir(ctx) == null) return "musl rootfs not installed";
        List<String> cmd = command(ctx, List.of("/bin/sh", "-lc", command),
                LocalRuntime.homeDir(ctx).getAbsolutePath());
        if (cmd == null) return "cannot build proot command";
        StringBuilder out = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                    if (out.length() > 1 << 20) break;
                }
            }
            if (!p.waitFor(120, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                out.append("\n[timeout]");
            }
        } catch (Exception e) {
            out.append("error: ").append(e.getMessage());
        }
        return out.toString();
    }
}
