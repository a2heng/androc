package com.a2heng.androc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 生成少量诊断命令（oc-doctor / oc-repair / oc-engine）。
 *
 * 现在只有一个运行时：android 插件下发的 musl rootfs。没有 Termux 前缀、
 * 没有 dpkg、没有可切换的引擎。
 */
public final class CommandResolver {

    public static final String SHELL_NAME = "oc-shell";
    public static final String PROOT_NAME = "oc-proot";
    public static final String NATIVE_ENV_NAME = "oc-native-env.sh";
    public static final String ENGINE_NAME = "oc-engine";
    public static final String DOCTOR_NAME = "oc-doctor";
    public static final String REPAIR_NAME = "oc-repair";

    private CommandResolver() {
    }

    public static void install(File home, File binDir) throws IOException {
        if (!binDir.isDirectory() && !binDir.mkdirs() && !binDir.isDirectory()) {
            throw new IOException("no se pudo crear " + binDir);
        }
        for (String old : new String[]{SHELL_NAME, PROOT_NAME, NATIVE_ENV_NAME}) {
            //noinspection ResultOfMethodCallIgnored
            new File(binDir, old).delete();
        }
        write(new File(binDir, DOCTOR_NAME), doctorScript());
        write(new File(binDir, REPAIR_NAME), repairScript());
        write(new File(binDir, ENGINE_NAME), engineScript());
    }

    private static void write(File file, String body) throws IOException {
        try (FileWriter w = new FileWriter(file, false)) {
            w.write(body);
        }
        //noinspection ResultOfMethodCallIgnored
        file.setExecutable(true, true);
        //noinspection ResultOfMethodCallIgnored
        file.setReadable(true, false);
    }

    /** oc-doctor：检查 musl rootfs 里的 Android 构建环境。 */
    private static String doctorScript() {
        return "#!/system/bin/sh\n"
                + "# Generado por CommandResolver. Uso: oc-doctor\n"
                + "echo \"== oc-doctor ==\"\n"
                + "echo \"engine=proot (musl rootfs)\"\n"
                + "echo \"HOME=$HOME\"\n"
                + "echo \"PWD=$PWD\"\n"
                + "echo \"JAVA_HOME=${JAVA_HOME:-<no definido>}\"\n"
                + "echo \"ANDROID_HOME=${ANDROID_HOME:-<no definido>}\"\n"
                + "echo \"-- binarios --\"\n"
                + "check() { if command -v \"$1\" >/dev/null 2>&1; then echo \"OK      $1 -> $(command -v \"$1\")\"; else echo \"MISSING $1\"; fi; }\n"
                + "check bash\n"
                + "check java\n"
                + "check aapt2\n"
                + "check zipalign\n"
                + "check apksigner\n"
                + "check gradle\n"
                + "echo \"-- versiones --\"\n"
                + "bash --version 2>&1 | head -1\n"
                + "java -version 2>&1 | head -1\n"
                + "aapt2 version 2>&1 | head -1\n"
                + "echo\n";
    }

    /** oc-repair：提示重装 android 插件（rootfs 由市场包下发）。 */
    private static String repairScript() {
        return "#!/system/bin/sh\n"
                + "# Generado por CommandResolver. Uso: oc-repair\n"
                + "echo \"oc-repair: rootfs 由 Android 插件下发。\"\n"
                + "echo \"请在 设置 → 组件与插件 里重新安装 Android 插件。\"\n";
    }

    /** oc-engine：没有引擎可切换。 */
    private static String engineScript() {
        return "#!/system/bin/sh\n"
                + "# Generado por CommandResolver. Todo corre en proot + musl rootfs.\n"
                + "case \"${1:-status}\" in\n"
                + "  status|get|\"\") echo proot ;;\n"
                + "  auto|proot) echo \"engine=proot (fijo; musl rootfs)\" ;;\n"
                + "  help|-h|--help)\n"
                + "    echo \"oc-engine: Androc corre todo dentro de proot; no hay modos.\"\n"
                + "    echo \"  oc-engine status -> proot\"\n"
                + "    echo \"  oc-doctor       -> diagnostico del entorno\" ;;\n"
                + "  *)\n"
                + "    echo \"uso: oc-engine [status|help]\" >&2\n"
                + "    exit 2 ;;\n"
                + "esac\n";
    }
}
