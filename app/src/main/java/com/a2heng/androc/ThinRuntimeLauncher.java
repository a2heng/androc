package com.a2heng.androc;

import android.content.Context;

import com.a2heng.androc.ui.data.LocalRuntimeLauncher;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/** Connects the shared UI dialog to the thin runtime process. */
public final class ThinRuntimeLauncher implements LocalRuntimeLauncher {
    private final Context context;

    public ThinRuntimeLauncher(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean getInstalled() {
        return ThinRuntime.installed(context);
    }

    @Override
    public String getVersion() {
        return ThinRuntime.installedVersion(context);
    }

    @Override
    public void start(Function2<? super String, ? super String, Unit> onReady,
                      Function1<? super String, Unit> onError) {
        ThinRuntimeService.start(context);
        new Thread(() -> {
            try {
                // 20s 内没等到健康检查也先交给 connect 试一次：
                // 避免首次点“本机”在这里干等 120s，取消再连才秒进。
                boolean ready = ThinRuntime.awaitReady(context, 20_000L);
                if (ready || ThinRuntime.running()) {
                    onReady.invoke("http://127.0.0.1:" + ThinRuntime.port(context),
                            LocalRuntime.serverPassword(context));
                } else {
                    String tail = ThinRuntime.logTail(context);
                    onError.invoke(tail.isEmpty() ? "服务没有启动起来" : tail);
                }
            } catch (Exception e) {
                onError.invoke(String.valueOf(e.getMessage()));
            }
        }, "thin-launcher").start();
    }

    @Override
    public void stop() {
        ThinRuntimeService.stop(context);
    }
}
