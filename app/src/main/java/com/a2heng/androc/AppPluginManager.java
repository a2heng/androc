package com.a2heng.androc;

import android.content.Context;
import android.content.Intent;
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
 * Plugin layers: a skill (or skill group) plus the toolchain it needs.
 *
 * <ul>
 *   <li><b>opencode</b> — opencode binary + proot userland; independent update
 *       against the upstream release.</li>
 *   <li><b>android-build</b> — Android skills + JDK/SDK/Gradle/aapt2.</li>
 *   <li><b>android-ai</b> — android-ai skill + AI runtimes.</li>
 *   <li><b>android-debug</b> — debug/install skills + adb.</li>
 *   <li><b>termux</b> — git/python/pkg in the prefix.</li>
 * </ul>
 */
public final class AppPluginManager implements PluginManager {
    private static final String PREF = "app";
    private static final String K_OPENCODE_LATEST = "upd_latest";
    private static final String K_PAYLOAD_VERSION = "plugin_tools_payload_version";

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, String> latest = new HashMap<>();

    public AppPluginManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public List<PluginInfo> list() {
        List<PluginInfo> out = new ArrayList<>();
        SharedPreferences sp = prefs();
        File home = LocalRuntime.homeDir(ctx);
        File prefix = LocalToolbox.prefix(ctx);
        File toolchain = ToolchainInstaller.root(ctx);
        long jdkBytes = dirSize(new File(prefix, "lib/jvm"));

        boolean opencode = LocalRuntime.prootAvailable(ctx) && LocalToolbox.complete(ctx);
        String opencodeInstalled = opencode
                ? firstNonEmpty(sp.getString("opencode_runtime_version", ""),
                BuildConfig.OPENCODE_VERSION)
                : "";
        String opencodeLatest = firstNonEmpty(latest.get(PluginInfo.OPENCODE),
                sp.getString(K_OPENCODE_LATEST, ""), BuildConfig.OPENCODE_VERSION);
        out.add(plugin(
                PluginInfo.OPENCODE,
                "OpenCode 内核",
                "本机 server、会话、agent",
                skillsFor(PluginInfo.OPENCODE),
                list("opencode " + BuildConfig.OPENCODE_VERSION, "proot"),
                opencodeInstalled,
                opencodeLatest,
                opencodeBinarySize(),
                opencode,
                true,
                false));

        List<String> termuxSkills = skillsFor(PluginInfo.TERMUX);
        boolean termux = new File(prefix, "bin/git").isFile()
                || new File(prefix, "bin/python3").isFile()
                || new File(prefix, "bin/pkg").isFile();
        long termuxBytes = Math.max(0, dirSize(prefix) - jdkBytes);
        out.add(plugin(
                PluginInfo.TERMUX,
                "Termux 工具",
                "shell、git、python、pkg",
                termuxSkills,
                list("bash / readline", "git", "python", "pkg / apt"),
                termux ? "1" : "",
                firstNonEmpty(latest.get(PluginInfo.TERMUX), "1"),
                termux ? human(termuxBytes) : "",
                termux,
                true,
                false));

        List<String> buildSkills = skillsFor(PluginInfo.ANDROID_BUILD);
        boolean buildSkillsInstalled = anySkill(home, buildSkills);
        boolean build = ToolchainInstaller.installed(ctx) && buildSkillsInstalled;
        String buildInstalled = ToolchainInstaller.installedVersion(ctx);
        String buildLatest = firstNonEmpty(latest.get(PluginInfo.ANDROID_BUILD),
                ToolchainInstaller.VERSION);
        out.add(plugin(
                PluginInfo.ANDROID_BUILD,
                "Android 构建",
                "端侧 assembleDebug / assembleRelease",
                buildSkills,
                list("JDK 21", "SDK 36", "aapt2", "Gradle 9.9", "Maven 缓存"),
                buildInstalled,
                buildLatest,
                build || ToolchainInstaller.root(ctx).isDirectory()
                        ? human(dirSize(toolchain) + jdkBytes)
                        : "",
                build,
                BuildConfig.TOOLS_BUNDLED,
                true));

        List<String> aiSkills = skillsFor(PluginInfo.ANDROID_AI);
        boolean ai = anySkill(home, aiSkills);
        out.add(plugin(
                PluginInfo.ANDROID_AI,
                "本地 AI",
                "ONNX / TFLite / llama.cpp / MNN",
                aiSkills,
                list("ONNX", "TFLite", "llama.cpp", "MNN"),
                ai ? "1" : "",
                firstNonEmpty(latest.get(PluginInfo.ANDROID_AI), "1"),
                skillSize(home, aiSkills),
                ai,
                true,
                true));

        List<String> debugSkills = skillsFor(PluginInfo.ANDROID_DEBUG);
        boolean debug = anySkill(home, debugSkills) && new File(prefix, "bin/adb").isFile();
        out.add(plugin(
                PluginInfo.ANDROID_DEBUG,
                "Android 调试",
                "adb、安装、日志、16 KB 检查",
                debugSkills,
                list("adb", "platform-tools"),
                debug ? "1" : "",
                firstNonEmpty(latest.get(PluginInfo.ANDROID_DEBUG), "1"),
                skillSize(home, debugSkills),
                debug,
                true,
                false));

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
        // list() reads from disk each time.
    }

    @Override
    public void checkUpdates(String id, Callback callback) {
        if (PluginInfo.OPENCODE.equals(id)) {
            new Thread(() -> {
                String found = "";
                try {
                    UpdateCheck.RuntimeAsset asset = UpdateCheck.opencodeRuntimeAsset();
                    if (asset != null && asset.version != null) found = asset.version;
                } catch (Throwable t) {
                    post(id, false, "检查更新失败：" + t.getMessage(), callback);
                    return;
                }
                if (found.isEmpty()) {
                    post(id, false, "没有拿到上游版本", callback);
                    return;
                }
                latest.put(id, found);
                prefs().edit().putString(K_OPENCODE_LATEST, found).apply();
                String installed = prefs().getString("opencode_runtime_version",
                        BuildConfig.OPENCODE_VERSION);
                boolean newer = UpdateCheck.cmpVer(found, installed) > 0;
                post(id, true,
                        newer ? "OpenCode 可更新到 " + found : "OpenCode 已是最新 " + found,
                        callback);
            }, "plugin-check-opencode").start();
            return;
        }
        // Tools packs have no remote manifest yet; compare with the bundled version.
        String available = PluginInfo.ANDROID_BUILD.equals(id)
                ? ToolchainInstaller.VERSION : "1";
        latest.put(id, available);
        post(id, true, "当前随 App 更新：" + available, callback);
    }

    @Override
    public void install(String id, Callback callback) {
        if (PluginInfo.ANDROID_BUILD.equals(id)) {
            new Thread(() -> {
                try {
                    PluginCatalog.setPluginEnabled(ctx, id, true);
                    ToolchainInstaller.ensure(ctx);
                    LocalRuntime.installBundledSkills(ctx);
                    boolean ok = ToolchainInstaller.installed(ctx)
                            && anySkill(LocalRuntime.homeDir(ctx), skillsFor(id));
                    post(id, ok, ok ? "Android 构建插件已安装" : "安装不完整，请重试",
                            callback);
                } catch (Throwable t) {
                    post(id, false, "安装失败：" + t.getMessage(), callback);
                }
            }, "plugin-install-build").start();
            return;
        }
        if (PluginInfo.ANDROID_AI.equals(id)) {
            new Thread(() -> {
                try {
                    PluginCatalog.setPluginEnabled(ctx, id, true);
                    LocalRuntime.installBundledSkills(ctx);
                    boolean ok = anySkill(LocalRuntime.homeDir(ctx), skillsFor(id));
                    post(id, ok, ok ? "本地 AI 插件已安装" : "安装不完整", callback);
                } catch (Throwable t) {
                    post(id, false, "安装失败：" + t.getMessage(), callback);
                }
            }, "plugin-install-ai").start();
            return;
        }
        if (PluginInfo.OPENCODE.equals(id)) {
            // The runtime is updated through its own service, not re-extracted.
            post(id, false, "OpenCode 请用“更新”检查上游版本", callback);
            return;
        }
        post(id, false, "这一层随内核提供，暂不支持单独安装", callback);
    }

    @Override
    public void update(String id, Callback callback) {
        if (PluginInfo.OPENCODE.equals(id)) {
            Intent i = new Intent(ctx, RuntimeUpdateService.class)
                    .setAction(RuntimeUpdateService.ACTION_INSTALL);
            try {
                ctx.startForegroundService(i);
                post(id, true, "已开始下载 OpenCode 更新，完成后会自动重启服务", callback);
            } catch (Throwable t) {
                post(id, false, "无法启动更新：" + t.getMessage(), callback);
            }
            return;
        }
        install(id, callback);
    }

    @Override
    public void uninstall(String id, Callback callback) {
        if (PluginInfo.ANDROID_BUILD.equals(id)) {
            new Thread(() -> {
                deleteTree(ToolchainInstaller.root(ctx));
                LocalRuntime.removePluginSkills(ctx, skillsFor(id));
                PluginCatalog.setPluginEnabled(ctx, id, false);
                post(id, true, "Android 构建插件已卸载；项目与会话保留", callback);
            }, "plugin-uninstall-build").start();
            return;
        }
        if (PluginInfo.ANDROID_AI.equals(id)) {
            new Thread(() -> {
                LocalRuntime.removePluginSkills(ctx, skillsFor(id));
                PluginCatalog.setPluginEnabled(ctx, id, false);
                post(id, true, "本地 AI 插件已卸载", callback);
            }, "plugin-uninstall-ai").start();
            return;
        }
        post(id, false, "内核/Termux 正在使用时不能卸载；请先停止本机服务器", callback);
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

    private static String skillSize(File home, List<String> skills) {
        long total = 0;
        for (String skill : skills) {
            total += dirSize(new File(home, ".config/opencode/skills/" + skill));
        }
        return total > 0 ? human(total) : "";
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private void post(String id, boolean ok, String message, Callback callback) {
        main.post(() -> {
            if (callback != null) callback.onPluginResult(id, ok, message);
        });
    }

    private static String installedSize(File dir) {
        return dir.isDirectory() ? human(dirSize(dir)) : "";
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
