package com.a2heng.androc;

import android.content.Context;
import android.util.Log;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.tukaani.xz.XZInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 内置 runtime-core（assets/runtime/rootfs.tar.xz）的解压与定位。
 *
 * bundle 自带这一份，所以装完就能起本机 OpenCode；thin 没有这个 asset，
 * 走市场安装的 runtime-core。
 */
final class RuntimeAssets {
    private static final String TAG = "OpenCodeRuntimeAssets";
    private static final String[] ASSETS = {"runtime/rootfs.tar.zst", "runtime/rootfs.tar.xz"};
    private static final String MARKER = ".installed-v2";
    /** 同一进程里可能同时有多个启动请求；解压必须串行，否则会互相删掉半成品。 */
    private static final Object LOCK = new Object();

    private RuntimeAssets() {
    }

    /** 已解压的 rootfs，或 null。 */
    static File rootfs(Context ctx) {
        File root = new File(ctx.getFilesDir(), "runtime-core");
        File marker = new File(root, MARKER);
        File fs = new File(root, "rootfs");
        return marker.isFile() && fs.isDirectory() ? fs : null;
    }

    /** bundle 里有内置 asset 但还没解压。 */
    static boolean pending(Context ctx) {
        return rootfs(ctx) == null && hasBundled(ctx);
    }

    static boolean available(Context ctx) {
        return rootfs(ctx) != null;
    }

    private static boolean hasBundled(Context ctx) {
        for (String a : ASSETS) { try { ctx.getAssets().open(a).close(); return true; } catch (Exception ignored) {} }
        return false;
    }
    private static String bundledAsset(Context ctx) {
        for (String a : ASSETS) { try { ctx.getAssets().open(a).close(); return a; } catch (Exception ignored) {} }
        return null;
    }

    /** 解压内置 runtime；已解压时直接返回。 */
    static File ensure(Context ctx) throws IOException {
        synchronized (LOCK) {
            File existing = rootfs(ctx);
            if (existing != null) return existing;
            File root = new File(ctx.getFilesDir(), "runtime-core");
            // 上一次解压可能被系统杀掉，留下半成品和断掉的 symlink；先清干净再从头解。
            deleteTree(root);
            if (!root.mkdirs() && !root.isDirectory()) throw new IOException("cannot create " + root);
            String asset = bundledAsset(ctx);
            if (asset == null) throw new IOException("no bundled runtime asset");
            Log.i(TAG, "extracting bundled runtime: " + asset);
            try (java.io.InputStream raw = new BufferedInputStream(ctx.getAssets().open(asset), 1 << 16);
                 java.io.InputStream dec = asset.endsWith(".zst")
                         ? new ZstdCompressorInputStream(raw)
                         : new XZInputStream(raw);
                 TarArchiveInputStream tar = new TarArchiveInputStream(dec)) {
                extract(tar, root);
            } catch (IOException | RuntimeException e) {
                // 不留下半个 rootfs，下一次启动可以重新解压。
                deleteTree(root);
                throw e;
            }
            File marker = new File(root, MARKER);
            //noinspection ResultOfMethodCallIgnored
            marker.createNewFile();
            Log.i(TAG, "bundled runtime ready: " + new File(root, "rootfs"));
            return new File(root, "rootfs");
        }
    }

    private static void extract(TarArchiveInputStream tar, File dest) throws IOException {
        String root = dest.getCanonicalPath() + File.separator;
        org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
        while ((entry = tar.getNextTarEntry()) != null) {
            File out = new File(dest, entry.getName());
            String canonical = out.getCanonicalPath();
            if (!canonical.equals(dest.getCanonicalPath()) && !canonical.startsWith(root)) {
                throw new IOException("unsafe tar entry " + entry.getName());
            }
            if (entry.isDirectory()) {
                if (!out.isDirectory() && !out.mkdirs() && !out.isDirectory()) {
                    throw new IOException("cannot create " + out);
                }
            } else if (entry.isSymbolicLink()) {
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                // File.exists() 会跟随断链返回 false，断链本身还在；必须按路径删。
                java.nio.file.Files.deleteIfExists(out.toPath());
                java.nio.file.Files.createSymbolicLink(
                        out.toPath(), java.nio.file.Paths.get(entry.getLinkName()));
            } else {
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                java.nio.file.Files.deleteIfExists(out.toPath());
                try (BufferedOutputStream os = new BufferedOutputStream(
                        new FileOutputStream(out), 1 << 16)) {
                    byte[] buf = new byte[1 << 16];
                    int n;
                    while ((n = tar.read(buf)) > 0) os.write(buf, 0, n);
                }
                if ((entry.getMode() & 0b001_000_000) != 0) out.setExecutable(true, true);
            }
        }
    }

    private static boolean deleteTree(File file) {
        if (file == null) return true;
        java.nio.file.Path path = file.toPath();
        try {
            // 断掉的 symlink 用 File.exists() 看不见，必须用 NOFOLLOW_LINKS。
            if (java.nio.file.Files.isSymbolicLink(path) ||
                    !java.nio.file.Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                java.nio.file.Files.deleteIfExists(path);
                return !java.nio.file.Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            }
            try (java.nio.file.DirectoryStream<java.nio.file.Path> kids =
                         java.nio.file.Files.newDirectoryStream(path)) {
                for (java.nio.file.Path kid : kids) deleteTree(kid.toFile());
            }
            java.nio.file.Files.deleteIfExists(path);
            return !java.nio.file.Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            Log.w(TAG, "cannot delete " + path + ": " + e.getMessage());
            return false;
        }
    }
}
