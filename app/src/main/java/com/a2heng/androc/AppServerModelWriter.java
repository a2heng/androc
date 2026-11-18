package com.a2heng.androc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.a2heng.androc.ui.ServerModelWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Fallback for servers that do not expose the experimental fs.write API:
 * write {@code model} into the local opencode.json and restart the server.
 *
 * <p>Only valid for the on-device kernel. Remote servers use the HTTP path in
 * {@code OcClient.setDefaultModel}.</p>
 */
public final class AppServerModelWriter implements ServerModelWriter {
    private static final String TAG = "OpenCodeLocal";
    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());

    public AppServerModelWriter(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public void setDefaultModel(String providerID, String modelID, String variant,
                                Callback callback) {
        new Thread(() -> {
            boolean ok = false;
            String message;
            try {
                File home = LocalRuntime.homeDir(ctx);
                String value = providerID + "/" + modelID
                        + (variant == null || variant.isEmpty() ? "" : "#" + variant);
                writeConfig(new File(home, ".config/opencode-android/opencode.json"), value);
                writeConfig(new File(home, ".config/opencode/opencode.json"), value);
                LocalRuntime.stop();
                LocalServerService.start(ctx);
                ok = true;
                message = "已写入服务器配置，正在重启";
            } catch (Throwable t) {
                Log.w(TAG, "no se pudo escribir el modelo por defecto", t);
                message = "写入配置失败：" + t.getMessage();
            }
            boolean result = ok;
            String text = message;
            main.post(() -> callback.onResult(result, text));
        }, "server-default-model").start();
    }

    private static void writeConfig(File file, String value) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new java.io.IOException("no se pudo crear " + parent);
        }
        JSONObject json = new JSONObject();
        if (file.isFile()) {
            try {
                String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                if (!raw.trim().isEmpty()) json = new JSONObject(raw);
            } catch (Exception ignored) {
            }
        }
        json.put("model", value);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
}
