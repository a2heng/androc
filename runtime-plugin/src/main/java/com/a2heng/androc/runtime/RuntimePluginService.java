package com.a2heng.androc.runtime;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.a2heng.androc.plugin.IRuntimePlugin;
import com.a2heng.androc.plugin.IRuntimePluginListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Headless runtime plugin service.
 *
 * <p>Current state: binding/metadata skeleton. The proot/opencode/Termux code
 * will move here from the old single-app module; until then start() reports
 * that the runtime is not migrated.</p>
 */
public final class RuntimePluginService extends Service {
    private volatile IRuntimePluginListener listener;

    private final IRuntimePlugin.Stub binder = new IRuntimePlugin.Stub() {
        @Override
        public String pluginInfo() {
            return readAsset("plugin.json");
        }

        @Override
        public String start() {
            // TODO: migrate LocalRuntime/LocalToolbox here and return the real
            // {"url":"http://127.0.0.1:4096","username":"opencode","password":"…","running":true}
            return "{\"running\":false,\"message\":\"runtime not migrated yet\"}";
        }

        @Override
        public void stop() {
            // TODO: stop the runtime process and foreground service.
        }

        @Override
        public String connection() {
            return "{\"running\":false}";
        }

        @Override
        public String pairingLink() {
            return "";
        }

        @Override
        public void setListener(IRuntimePluginListener value) {
            listener = value;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "{\"id\":\"opencode\",\"name\":\"OpenCode 运行时\",\"version\":\"0.1.0\"}";
        }
    }
}
