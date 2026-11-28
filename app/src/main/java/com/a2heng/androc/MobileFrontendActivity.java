package com.a2heng.androc;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Host ligero del frontend Compose embebido (:ui interno de Androc).
 *
 * <p>No carga WebView ni el layout clasico: solo arranca el runtime local,
 * espera a que el servicio responda y monta la UI Compose. Es el modo
 * "un APK = backend + frontend" de Androc.</p>
 */
public class MobileFrontendActivity extends AppCompatActivity {
    private static final String TAG = "OpenCodeLocal";
    private static final int PORT = BuildConfig.SERVER_PORT;
    private static final String URL = "http://127.0.0.1:" + PORT;
    private static final long HEALTH_TIMEOUT_MS = 25_000L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean mounted;
    private boolean starting;
    private volatile boolean preparingCancelled;
    private TextView status;
    private androidx.activity.result.ActivityResultLauncher<String> notifPermLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 临时调试接口：只允许 debug 构建，release 绝不能有监听。
        if (BuildConfig.DEBUG) DebugApi.start(this);
        if (BuildConfig.DEBUG) AutomationServer.start(this);
        if (!"mobile".equals(BuildConfig.UI_MODE)) {
            // Respaldo: el frontend WebView clasico.
            startActivity(new android.content.Intent(this, MainActivity.class));
            finish();
            return;
        }
        // Igual que el APK independiente de OpenCode Mobile: edge-to-edge +
        // adjustNothing; el IME se gestiona con imePadding() en Compose.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat c =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.setSystemBarsBehavior(
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            boolean dark = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            c.setAppearanceLightStatusBars(!dark);
            c.setAppearanceLightNavigationBars(!dark);
        }
        setContentView(buildLoading());
        // 自更新要选对发行版：thin 只下 thin.apk，bundle 只下 bundle.apk。
        com.a2heng.androc.ui.data.AppBuild.INSTANCE.setThin(BuildConfig.THIN);
        // 连接保活是前台服务：Android 13+ 没有通知权限时通知栏不显示，
        // 部分 ROM 会更快回收进程。登录页的独立客户端会主动申请，这里补齐。
        notifPermLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                granted -> { });
        requestNotifPermission();
        // 先准备内置包（只解压一次），再显示 UI；本机 server 仍然等用户点
        // 「本机 → 启动并登录」才启动。准备和连接分成两步，各自有固定展示。
        // 每次启动都先 ensure（已解压时立即返回）：保证不会在解压完成前
        // 自动恢复上次的本机连接。
        showPreparation();
    }

    private void requestNotifPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return;
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        android.content.SharedPreferences sp = getSharedPreferences("app", MODE_PRIVATE);
        if (sp.getBoolean("notif_perm_asked", false)) return;
        sp.edit().putBoolean("notif_perm_asked", true).apply();
        try {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } catch (Exception ignored) {
        }
    }

    private LinearLayout buildLoading() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0E1116"));
        ProgressBar bar = new ProgressBar(this);
        root.addView(bar, new LinearLayout.LayoutParams(dp(42), dp(42)));
        status = new TextView(this);
        status.setText("正在启动 opencode…");
        status.setTextColor(Color.parseColor("#C7CDD6"));
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        root.addView(status, lp);
        android.widget.Button cancel = new android.widget.Button(this);
        cancel.setText("取消");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(v -> {
            preparingCancelled = true;
            finish();
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(18);
        root.addView(cancel, clp);
        return root;
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    /** 内置 runtime / Android 工具链是否已经解压好。 */
    private boolean needsPreparation() {
        if (BuildConfig.THIN) return false;
        boolean runtime = RuntimeAssets.rootfs(this) != null;
        boolean android = !BuildConfig.HAS_ANDROID_TOOLS || AndroidAssets.rootfs(this) != null;
        return !runtime || !android;
    }

    /** 首次/升级后：单独的解压页，带取消；完成后再进登录/连接 UI。 */
    private void showPreparation() {
        // ui flavor 是纯客户端：没有本地 runtime，直接进登录/连接页。
        if (BuildConfig.THIN) {
            showFrontend();
            return;
        }
        preparingCancelled = false;
        setContentView(buildLoading());
        if (status != null) status.setText("正在解压运行环境…");
        new Thread(() -> {
            try {
                RuntimeAssets.ensure(MobileFrontendActivity.this);
                AndroidAssets.ensure(MobileFrontendActivity.this);
                if (!preparingCancelled) ui.post(this::showFrontend);
            } catch (Exception e) {
                ui.post(() -> {
                    if (status != null) status.setText("准备失败：" + e.getMessage());
                });
            }
        }, "androc-prepare").start();
    }

    private void startServerAndShow() {
        if (starting || mounted) return;
        starting = true;
        LocalServerService.start(this);
        new Thread(() -> {
            long deadline = System.currentTimeMillis() + HEALTH_TIMEOUT_MS;
            boolean ok = false;
            while (System.currentTimeMillis() < deadline) {
                if (probe()) {
                    ok = true;
                    break;
                }
                try {
                    Thread.sleep(600);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
            final boolean healthy = ok;
            ui.post(() -> {
                starting = false;
                if (healthy) {
                    showFrontend();
                } else if (status != null) {
                    status.setText("服务没有启动起来，退出重进一次（或查看日志 OpenCodeLocal）");
                }
            });
        }, "mobile-health").start();
    }

    private boolean probe() {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(URL + "/api/info").openConnection();
            c.setConnectTimeout(1200);
            c.setReadTimeout(2000);
            c.setUseCaches(false);
            String auth = Base64.encodeToString(
                    ("opencode:" + LocalRuntime.serverPassword(this)).getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP);
            c.setRequestProperty("Authorization", "Basic " + auth);
            return c.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private void showFrontend() {
        if (mounted) return;
        mounted = true;
        String pwd = LocalRuntime.serverPassword(this);
        try {
            // Expose the local component to the shared login/settings dialog.
            // Full builds bundle it; thin builds leave the launcher null.
            com.a2heng.androc.ui.data.LocalRuntimeHost.INSTANCE.setLauncher(
                    new com.a2heng.androc.ui.data.LocalRuntimeLauncher() {
                        @Override
                        public boolean getInstalled() {
                            // bundle 自带 runtime + OC，永远算已安装；
                            // thin 只有下载/解压后才算。
                            return !BuildConfig.THIN
                                    || com.a2heng.androc.RuntimeAssets.available(
                                            MobileFrontendActivity.this)
                                    || com.a2heng.androc.MuslRootfs.available(
                                            MobileFrontendActivity.this);
                        }

                        @Override
                        public String getVersion() {
                            return BuildConfig.OPENCODE_VERSION;
                        }

                        @Override
                        public void start(
                                kotlin.jvm.functions.Function2<? super String, ? super String, kotlin.Unit> onReady,
                                kotlin.jvm.functions.Function1<? super String, kotlin.Unit> onError) {
                            new Thread(() -> {
                                LocalServerService.start(MobileFrontendActivity.this);
                                long deadline = System.currentTimeMillis() + HEALTH_TIMEOUT_MS;
                                while (System.currentTimeMillis() < deadline) {
                                    if (probe()) {
                                        onReady.invoke(URL,
                                                LocalRuntime.serverPassword(MobileFrontendActivity.this));
                                        return;
                                    }
                                    try {
                                        Thread.sleep(600);
                                    } catch (InterruptedException e) {
                                        return;
                                    }
                                }
                                // 健康检查可能比 OkHttp 严格；到时间也让 connect 试一次，
                                // 避免第一次点“本机”一直空等。
                                onReady.invoke(URL,
                                        LocalRuntime.serverPassword(MobileFrontendActivity.this));
                            }, "local-launcher").start();
                        }

                        @Override
                        public void stop() {
                            LocalServerService.stop(MobileFrontendActivity.this);
                        }
                    });
            // El explorador y los terminales usan el sistema de archivos real:
            // "/" es la raiz Android y "~" el HOME de la app. Antes se pasaba
            // un rootfs de enlaces, pero el servidor v2 no lista symlinks y
            // dejaba la raiz vacia.
            // 本机 Git：直接跑 proot 里的 git，不再走 OpenCode 的 location 缓存。
            com.a2heng.androc.ui.data.LocalGitHost.INSTANCE.setRunner(
                    (command, timeout) -> com.a2heng.androc.LocalRuntime.runInSandbox(
                            MobileFrontendActivity.this, command, timeout, 4_000_000));
            LocalRuntime.workspaceDir(this);
            com.a2heng.androc.ui.LocalShellSpawner shell = () -> {
                try {
                    return LocalRuntime.localShell(this);
                } catch (Throwable t) {
                    Log.w(TAG, "no se pudo abrir el shell local: " + t);
                    return null;
                }
            };
            // El explorador arranca en HOME, no en el "/" real: la app no
            // puede leer la raiz Android y esa vista solo mostraba vacio.
            // El modelo por defecto es el del servidor; este writer es el
            // fallback local cuando el servidor no expone fs.write.
            com.a2heng.androc.ui.OpenCodeMobileHost.setServerModelWriter(
                    new AppServerModelWriter(this));
            com.a2heng.androc.ui.OpenCodeMobileHost.show(
                    this, BuildConfig.THIN ? "" : URL, pwd, shell,
                    LocalRuntime.homeDir(this).getAbsolutePath(),
                    true, true);
        } catch (Throwable t) {
            Log.w(TAG, "frontend Compose fallo; se abre el WebView", t);
            mounted = false;
            startActivity(new android.content.Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si el proceso se recreo o el servicio aun no estaba listo, reintenta solo.
        // Thin builds never start the local server.
        // 不在 onResume 自动拉起本机 server；由用户显式启动。
    }
}
