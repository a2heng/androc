package com.a2heng.androc;

import android.content.Context;
import android.util.Log;

import com.a2heng.androc.market.MarketClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Directory packs downloaded from the market (skills + payload).
 *
 * Installed layout: files/packs/&lt;id&gt;/&lt;version&gt;/ (the tar contents).
 * Android / language / writing skills live inside the pack and are copied
 * into the opencode config after install, so the APK itself stays UI-only.
 */
final class PackInstaller {
    private static final String TAG = "OpenCodePacks";

    private PackInstaller() {
    }

    static File install(Context ctx, String packId) throws IOException {
        return new MarketClient().installPack(ctx.getFilesDir(), packId, null);
    }

    /** Latest installed version directory for a pack, or null. */
    static File installedDir(Context ctx, String packId) {
        File parent = new File(ctx.getFilesDir(), "packs/" + packId);
        File[] dirs = parent.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) return null;
        File best = dirs[0];
        for (File dir : dirs) {
            if (dir.getName().compareTo(best.getName()) > 0) best = dir;
        }
        return best;
    }

    /** Skill directory names inside an installed pack. */
    static List<String> skillNames(File packDir) {
        List<String> out = new ArrayList<>();
        if (packDir == null) return out;
        File[] children = new File(packDir, "skills").listFiles(File::isDirectory);
        if (children == null) return out;
        for (File child : children) out.add(child.getName());
        return out;
    }

    /** The two opencode config roots the app mirrors skills into. */
    static List<File> configDirs(Context ctx) {
        File home = LocalRuntime.homeDir(ctx);
        return Arrays.asList(
                new File(home, ".config/opencode"),
                new File(home, ".config/opencode-android"));
    }

    /** Copy packDir/skills/* into configDir/skills/*. */
    static void copySkills(File packDir, File configDir) {
        if (packDir == null) return;
        File src = new File(packDir, "skills");
        File dest = new File(configDir, "skills");
        File[] children = src.listFiles(File::isDirectory);
        if (children == null) return;
        for (File child : children) {
            try {
                copyTree(child, new File(dest, child.getName()));
            } catch (IOException e) {
                Log.w(TAG, "copy skill failed: " + child.getName(), e);
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
}
