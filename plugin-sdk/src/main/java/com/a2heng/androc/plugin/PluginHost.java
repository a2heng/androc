package com.a2heng.androc.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.a2heng.androc.contracts.RuntimeConnection;

/**
 * Bind a headless runtime plugin APK and obtain its loopback connection.
 *
 * <p>Usage from the thin UI host:</p>
 * <pre>
 * PluginHost host = PluginHost.bind(context, "com.a2heng.androc.runtime", callback);
 * host.start();
 * // callback.onConnected(connection) -> show Compose UI
 * </pre>
 */
public final class PluginHost implements ServiceConnection {
    public interface Callback {
        void onConnected(RuntimeConnection connection);

        void onError(String message);
    }

    private final Context ctx;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile IRuntimePlugin plugin;
    private volatile boolean closed;
    private Runnable pendingStart;

    private PluginHost(Context ctx, Callback callback) {
        this.ctx = ctx.getApplicationContext();
        this.callback = callback;
    }

    public static PluginHost bind(Context ctx, String packageName, Callback callback) {
        PluginHost host = new PluginHost(ctx, callback);
        Intent intent = new Intent(PluginContract.ACTION_BIND).setPackage(packageName);
        boolean ok;
        try {
            ok = host.ctx.bindService(intent, host, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            ok = false;
        }
        if (!ok) {
            callback.onError("无法绑定运行时插件 " + packageName);
        }
        return host;
    }

    public void start() {
        Runnable task = () -> {
            IRuntimePlugin p = plugin;
            if (p == null) {
                main.post(() -> callback.onError("运行时插件还没有绑定"));
                return;
            }
            try {
                String raw = p.start();
                PluginConnection connection = PluginConnection.parse(raw);
                if (connection.running && !connection.url.isEmpty()) {
                    RuntimeConnection rc = connection.toRuntimeConnection();
                    main.post(() -> callback.onConnected(rc));
                } else {
                    String message = connection.message.isEmpty()
                            ? "运行时没有启动" : connection.message;
                    main.post(() -> callback.onError(message));
                }
            } catch (Throwable t) {
                main.post(() -> callback.onError(String.valueOf(t.getMessage())));
            }
        };
        IRuntimePlugin p = plugin;
        if (p == null) {
            pendingStart = task;
        } else {
            new Thread(task, "plugin-start").start();
        }
    }

    public void stop() {
        IRuntimePlugin p = plugin;
        if (p == null) return;
        try {
            p.stop();
        } catch (Throwable ignored) {
        }
    }

    public void close() {
        closed = true;
        try {
            ctx.unbindService(this);
        } catch (Exception ignored) {
        }
        plugin = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (closed) return;
        plugin = IRuntimePlugin.Stub.asInterface(service);
        Runnable task = pendingStart;
        pendingStart = null;
        if (task != null) {
            new Thread(task, "plugin-start").start();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        plugin = null;
        if (!closed) {
            main.post(() -> callback.onError("运行时插件连接断开"));
        }
    }

    @Override
    public void onBindingDied(ComponentName name) {
        onServiceDisconnected(name);
    }
}
