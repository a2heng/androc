package com.a2heng.androc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.a2heng.androc.contracts.PluginInfo;
import com.a2heng.androc.contracts.PluginManager;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 插件层（只保留两层）：
 *
 * <ul>
 *   <li><b>opencode</b> — server 二进制；bundle 内置，thin 从市场下载。</li>
 *   <li><b>android</b> — android-* skills + musl rootfs（JDK/SDK/Gradle/aapt2）。</li>
 * </ul>
 *
 * 没有 Termux、没有 bionic、没有第二套 rootfs。
 */
public final class AppPluginManager implements PluginManager {
    private static final String PREF = "app";
    private static final String K_OPENCODE_LATEST = "upd_latest";

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, String> latest = new HashMap<>();

    public AppPluginManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public List<PluginInfo> list() {
        List<PluginInfo> out = new ArrayList<>();
        if (BuildConfig.THIN) return out; // thin 没有市场
        SharedPreferences sp = prefs();
        File home = LocalRuntime.homeDir(ctx);

        // android：skills + musl rootfs
        File rootfs = MuslRootfs.dir(ctx);
        List<String> androidSkills = skillsFor(PluginInfo.ANDROID);
        boolean androidInstalled = rootfs != null && anySkill(home, androidSkills);
        String androidVersion = rootfs != null ? rootfs.getParentFile().getName() : "";
        long androidBytes = dirSize(rootfs) + skillBytes(home, androidSkills);
        out.add(plugin(
                PluginInfo.ANDROID,
                "Android 开发",
                "构建、调试、AI、测试：skills + musl rootfs",
                androidSkills,
                list("musl/Alpine", "JDK 21", "SDK 36", "Gradle 9.9", "aapt2", "adb"),
                androidVersion,
                firstNonEmpty(latest.get(PluginInfo.ANDROID), androidVersion),
                androidBytes > 0 ? human(androidBytes) : "",
                androidInstalled,
                false,
                true));

        return out;
    }

    private String opencodeBinarySize() {
        String path = prefs().getString("opencode_runtime_path", "");
        File file = path.isEmpty() ? null : new File(path);
        if (file == null || !file.isFile()) {
            file = new File(ctx.getApplicationInfo().nativeLibraryDir,
                    "libopencode2_bin.so");
        }
        return file.isFile() ? human(file.length()) : "";
    }

    @Override
    public void refresh() {
        // list() 每次都从磁盘读。
    }

    @Override
    public void checkUpdates(String id, Callback callback) {
        post(id, true, "Android 插件由市场更新；重新安装即可", callback);
    }

    @Override
    public void install(String id, Callback callback) {
        if (PluginInfo.ANDROID.equals(id)) {
            new Thread(() -> {
                try {
                    File pack = PackInstaller.install(ctx, id);
                    if (PluginInfo.ANDROID.equals(id)) {
                        for (File config : PackInstaller.configDirs(ctx)) {
                            PackInstaller.copySkills(pack, config);
                        }
                    }
                    boolean ok = anySkill(LocalRuntime.homeDir(ctx), skillsFor(id));
                    post(id, ok, ok ? id + " 已安装" : "安装不完整，请重试", callback);
                } catch (Throwable t) {
                    post(id, false, "安装失败：" + t.getMessage(), callback);
                }
            }, "plugin-install-" + id).start();
            return;
        }
        post(id, false, "未知插件：" + id, callback);
    }

    @Override
    public void update(String id, Callback callback) {
        install(id, callback);
    }

    @Override
    public void uninstall(String id, Callback callback) {
        if (PluginInfo.ANDROID.equals(id)) {
            new Thread(() -> {
                deleteTree(new File(ctx.getFilesDir(), "packs/" + id));
                if (PluginInfo.ANDROID.equals(id)) {
                    LocalRuntime.removePluginSkills(ctx, skillsFor(id));
                }
                post(id, true, id + " 已卸载", callback);
            }, "plugin-uninstall-" + id).start();
            return;
        }
        post(id, false, "内核正在使用时不能卸载；请先停止本机服务器", callback);
    }

    @Override
    public void importFrom(String uri, Callback callback) {
        post(null, false, "从文件导入会在 pack 格式落地后启用", callback);
    }

    private List<String> skillsFor(String id) {
        return PluginCatalog.skillsFor(ctx, id);
    }

    private PluginInfo plugin(String id, String name, String summary,
                              List<String> skills, List<String> toolchain,
                              String version, String latestVersion, String sizeText,
                              boolean installed, boolean bundled, boolean removable) {
        boolean update = installed
                && version != null && !version.isEmpty()
                && latestVersion != null && !latestVersion.isEmpty()
                && UpdateCheck.cmpVer(latestVersion, version) > 0;
        return new PluginInfo(id, name, summary, skills, toolchain, version,
                latestVersion, sizeText, installed, bundled, removable, update);
    }

    private static List<String> list(String... values) {
        List<String> out = new ArrayList<>();
        for (String value : values) if (value != null && !value.isEmpty()) out.add(value);
        return out;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) return value;
        }
        return "";
    }

    private static boolean anySkill(File home, List<String> skills) {
        for (String skill : skills) {
            if (new File(home, ".config/opencode/skills/" + skill).isDirectory()) return true;
        }
        return false;
    }

    private static long skillBytes(File home, List<String> skills) {
        long total = 0;
        for (String skill : skills) {
            total += dirSize(new File(home, ".config/opencode/skills/" + skill));
        }
        return total;
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private void post(String id, boolean ok, String message, Callback callback) {
        main.post(() -> {
            if (callback != null) callback.onPluginResult(id, ok, message);
        });
    }

    private static long dirSize(File file) {
        if (file == null || !file.exists()) return 0;
        try {
            if (Files.isSymbolicLink(file.toPath())) return file.length();
        } catch (Exception ignored) {
            return 0;
        }
        if (file.isFile()) return file.length();
        File[] kids = file.listFiles();
        if (kids == null) return 0;
        long total = 0;
        for (File kid : kids) total += dirSize(kid);
        return total;
    }

    private static String human(long bytes) {
        if (bytes <= 0) return "";
        double gb = bytes / 1073741824.0;
        if (gb >= 1.0) return String.format(Locale.US, "%.1f GB", gb);
        double mb = bytes / 1048576.0;
        if (mb >= 1.0) return String.format(Locale.US, "%.0f MB", mb);
        return String.format(Locale.US, "%.0f KB", bytes / 1024.0);
    }

    private static boolean deleteTree(File file) {
        if (file == null || !file.exists()) return true;
        try {
            if (Files.isSymbolicLink(file.toPath())) return file.delete() || !file.exists();
        } catch (Exception ignored) {
            return false;
        }
        File[] kids = file.listFiles();
        if (kids != null) {
            for (File kid : kids) deleteTree(kid);
        }
        return file.delete() || !file.exists();
    }
}
