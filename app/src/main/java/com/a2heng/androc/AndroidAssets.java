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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * full flavor 内置的 Android 工具链包（assets/android/pack.tar.xz）。
 * 包结构与市场 android 包一致：顶层含 skills/ 和 rootfs/。
 */
final class AndroidAssets {
    private static final String TAG = "OpenCodeAndroidAssets";
    static final String BUNDLED_VERSION = "bundled";
    private static final String[] ASSETS = {"android/pack.tar.zst", "android/pack.tar.xz"};
    private static final String MARKER = ".installed-v1";
    private static final Object LOCK = new Object();

    private AndroidAssets() {
    }

    static File packDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "packs/android/" + BUNDLED_VERSION);
        File marker = new File(dir, MARKER);
        return marker.isFile() && new File(dir, "rootfs").isDirectory() ? dir : null;
    }

    static File rootfs(Context ctx) {
        File dir = packDir(ctx);
        if (dir == null) return null;
        File fs = new File(dir, "rootfs");
        return fs.isDirectory() ? fs : null;
    }

    static boolean hasBundled(Context ctx) {
        for (String a : ASSETS) {
            try { ctx.getAssets().open(a).close(); return true; } catch (Exception ignored) {}
        }
        return false;
    }

    private static String bundledAsset(Context ctx) {
        for (String a : ASSETS) {
            try { ctx.getAssets().open(a).close(); return a; } catch (Exception ignored) {}
        }
        return null;
    }

    static boolean available(Context ctx) {
        return rootfs(ctx) != null || hasBundled(ctx);
    }

    static File ensure(Context ctx) throws IOException {
        synchronized (LOCK) {
            File existing = rootfs(ctx);
            if (existing != null) {
                ensureSkillsInstalled(ctx, packDir(ctx));
                return existing;
            }
            if (!hasBundled(ctx)) return null;
            File parent = new File(ctx.getFilesDir(), "packs/android");
            File dir = new File(parent, BUNDLED_VERSION);
            deleteTree(dir);
            if (!dir.mkdirs() && !dir.isDirectory()) throw new IOException("cannot create " + dir);
            String asset = bundledAsset(ctx);
            if (asset == null) return null;
            Log.i(TAG, "extracting bundled android pack: " + asset);
            try (InputStream raw = new BufferedInputStream(ctx.getAssets().open(asset), 1 << 16);
                 InputStream dec = asset.endsWith(".zst")
                         ? new ZstdCompressorInputStream(raw)
                         : new XZInputStream(raw);
                 TarArchiveInputStream tar = new TarArchiveInputStream(dec)) {
                extract(tar, dir);
            } catch (IOException | RuntimeException e) {
                deleteTree(dir);
                throw e;
            }
            File marker = new File(dir, MARKER);
            //noinspection ResultOfMethodCallIgnored
            marker.createNewFile();
            ensureSkillsInstalled(ctx, dir);
            Log.i(TAG, "bundled android pack ready");
            return new File(dir, "rootfs");
        }
    }

    /** 把包里的 skills 复制到两个 opencode 配置目录。 */
    static void ensureSkillsInstalled(Context ctx, File dir) {
        if (dir == null) return;
        File src = new File(dir, "skills");
        File[] children = src.listFiles(File::isDirectory);
        if (children == null || children.length == 0) return;
        File home = LocalRuntime.homeDir(ctx);
        File[] configDirs = new File[] {
                new File(home, ".config/opencode"),
                new File(home, ".config/opencode-android"),
        };
        for (File config : configDirs) {
            File dest = new File(config, "skills");
            for (File child : children) {
                try {
                    copyTree(child, new File(dest, child.getName()));
                } catch (IOException e) {
                    Log.w(TAG, "copy skill failed: " + child.getName(), e);
                }
            }
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

    private static void copyTree(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.isDirectory() && !dest.mkdirs() && !dest.isDirectory()) {
                throw new IOException("cannot create " + dest);
            }
            File[] children = src.listFiles();
            if (children == null) return;
            for (File child : children) copyTree(child, new File(dest, child.getName()));
        } else {
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
        }
    }

    private static boolean deleteTree(File file) {
        if (file == null) return true;
        java.nio.file.Path path = file.toPath();
        try {
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
