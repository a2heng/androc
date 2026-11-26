package com.a2heng.androc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Chequeo diario de updates del launcher (no del runtime: va dentro del APK). */
public class UpdateCheckReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ServerWatch.rearmUpdateCheck(ctx);
            return;
        }
        ServerWatch.armUpdateCheck(ctx);
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                String[] rel = UpdateCheck.launcherRelease();
                String cur = UpdateCheck.appVersion(ctx);
                if (rel != null && !cur.isEmpty() && UpdateCheck.cmpVer(rel[0], cur) > 0
                        && !rel[0].equals(UpdateCheck.p(ctx).getString("appupd_notified", ""))) {
                    if (ServerWatch.notifyId(ctx, ServerWatch.NID_APP,
                            ServerWatch.t(ctx, "Nueva versión de la app", "New app version"),
                            ServerWatch.t(ctx, "Disponible ", "Available ") + rel[0]
                                    + ServerWatch.t(ctx, ": abre la app para descargarla",
                                            ": open the app to download it"))) {
                        UpdateCheck.p(ctx).edit().putString("appupd_notified", rel[0]).apply();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                pr.finish();
            }
        }).start();
    }
}
