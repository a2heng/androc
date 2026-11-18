package com.a2heng.androc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/** Foreground host for the thin OpenCode process. */
public class ThinRuntimeService extends Service {
    private static final String ACTION_START = "com.a2heng.androc.THIN_START";
    private static final String ACTION_STOP = "com.a2heng.androc.THIN_STOP";
    private static final String CHANNEL = "thin_runtime";
    private static final int NID = 2210;

    public static void start(Context ctx) {
        ContextCompat.startForegroundService(ctx, new Intent(ctx, ThinRuntimeService.class)
                .setAction(ACTION_START));
    }

    public static void stop(Context ctx) {
        try {
            ctx.startService(new Intent(ctx, ThinRuntimeService.class).setAction(ACTION_STOP));
        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            ThinRuntime.stop();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NID, notification("OpenCode 正在启动…"));
        new Thread(() -> {
            try {
                ThinRuntime.start(getApplicationContext());
                if (ThinRuntime.awaitReady(getApplicationContext(), 120_000L)) {
                    update("OpenCode 已在本机运行");
                    Log.i("ThinRuntimeService", "ready on 127.0.0.1:" + ThinRuntime.port(getApplicationContext()));
                } else {
                    Log.w("ThinRuntimeService", "health check failed: " + ThinRuntime.logTail(this));
                    update("OpenCode 启动失败");
                    ThinRuntime.stop();
                    stopSelf();
                }
            } catch (Exception e) {
                Log.w("ThinRuntimeService", "start failed", e);
                update("OpenCode 启动失败");
                stopSelf();
            }
        }, "thin-runtime").start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        ThinRuntime.stop();
        super.onDestroy();
    }

    private void update(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NID, notification(text));
    }

    private Notification notification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "本机 OpenCode", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(this, MobileFrontendActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Androc")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }
}
