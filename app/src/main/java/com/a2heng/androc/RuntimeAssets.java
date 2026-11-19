package com.a2heng.androc;

import android.content.Context;
import android.util.Log;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
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
    private static final String ASSET = "runtime/rootfs.tar.xz";
    private static final String MARKER = ".installed-v2";

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
        try {
            ctx.getAssets().open(ASSET).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 解压内置 runtime；已解压时直接返回。 */
    static File ensure(Context ctx) throws IOException {
        File existing = rootfs(ctx);
        if (existing != null) return existing;
        File root = new File(ctx.getFilesDir(), "runtime-core");
        deleteTree(root);
        if (!root.mkdirs() && !root.isDirectory()) throw new IOException("cannot create " + root);
        Log.i(TAG, "extracting bundled runtime...");
        // The tar already starts with `rootfs/`, so extract into
        // files/runtime-core and the final path is runtime-core/rootfs.
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new XZInputStream(new BufferedInputStream(
                        ctx.getAssets().open(ASSET), 1 << 16)))) {
            extract(tar, root);
        }
        File marker = new File(root, MARKER);
        //noinspection ResultOfMethodCallIgnored
        marker.createNewFile();
        Log.i(TAG, "bundled runtime ready: " + new File(root, "rootfs"));
        return new File(root, "rootfs");
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
                if (out.exists()) out.delete();
                java.nio.file.Files.createSymbolicLink(
                        out.toPath(), java.nio.file.Paths.get(entry.getLinkName()));
            } else {
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
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
        if (file == null || !file.exists()) return true;
        File[] kids = file.listFiles();
        if (kids != null) for (File kid : kids) deleteTree(kid);
        return file.delete() || !file.exists();
    }
}
