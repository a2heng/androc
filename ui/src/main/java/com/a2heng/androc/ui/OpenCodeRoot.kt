package com.a2heng.androc.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.AutomationBridge
import com.a2heng.androc.ui.data.SharedPayload
import com.a2heng.androc.ui.ui.AppRoot
import com.a2heng.androc.ui.ui.OcTheme
import com.a2heng.androc.contracts.PluginManager
import java.lang.Process

/** 宿主（Androc）提供的本机 shell 进程（Termux/proot 里的 bash）。 */
fun interface LocalShellSpawner {
    fun spawn(): Process?
}

/** 本机终端能力：由宿主注入；独立 APK 里为 null，终端页只有服务端 PTY。 */
object LocalShellHost {
    @Volatile
    var spawner: LocalShellSpawner? = null
    val available: Boolean get() = spawner != null
}

/** 修改服务器默认模型的宿主能力；只有本机内核能实现。 */
interface ServerModelWriter {
    fun setDefaultModel(
        providerID: String,
        modelID: String,
        variant: String?,
        callback: Callback,
    )

    fun interface Callback {
        fun onResult(ok: Boolean, message: String)
    }
}

object ServerModelHost {
    @Volatile
    var writer: ServerModelWriter? = null
}

/**
 * 整个 Compose 客户端的根组合。
 *
 * - 独立 APK：`serverUrl = null`，走保存的服务器 / 登录页。
 * - Androc（单 APK）：传入本机 opencode 的 URL、随机密码，以及可选的
 *   本机 shell 与文件根目录。
 */
@Composable
fun OpenCodeMobileRoot(
    serverUrl: String? = null,
    password: String? = null,
    shared: SharedPayload? = null,
    fsRoot: String? = null,
    kernelBundled: Boolean = false,
    toolsBundled: Boolean = false,
    plugins: PluginManager? = null,
    onSharedConsumed: () -> Unit = {},
) {
    val model: AppModel = viewModel()
    DisposableEffect(model) {
        AutomationBridge.model = model
        onDispose {
            if (AutomationBridge.model === model) AutomationBridge.model = null
        }
    }
    model.overrideFsRoot(fsRoot)
    model.setBundled(kernelBundled, toolsBundled)
    model.setPluginManager(plugins)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // 启动时的 connect 可能早于应用可见，保活服务会启动失败；回到前台补一次。
        model.ensureKeepAlive()
        model.refreshOnResume()
    }
    OcTheme(themeMode = model.themeMode, appearance = model.appearance) {
        LaunchedEffect(shared) {
            shared?.let {
                model.applyShare(it.text, it.image)
                onSharedConsumed()
            }
        }
        LaunchedEffect(serverUrl, password) {
            val last = model.serverUrl
            when {
                // 上次连的是远程：优先恢复它，不要被宿主的本机 URL 覆盖。
                last.isNotBlank() && !model.isLocalUrl(last) ->
                    model.connect(last, model.password, remember = false)
                // 上次是本机（或首次启动）：用宿主刚启动的本机 URL/密码。
                !serverUrl.isNullOrBlank() ->
                    model.connect(serverUrl, password.orEmpty(), remember = false)
                else -> model.autoConnect()
            }
        }
        AppRoot(model)
    }
}

/**
 * 给宿主（Androc 的 Java Activity）用的入口：
 * 启动本地 opencode 之后直接调用它，整个界面就是这套 Compose 客户端。
 */
object OpenCodeMobileHost {
    @JvmStatic
    fun setServerModelWriter(writer: ServerModelWriter?) {
        ServerModelHost.writer = writer
    }

    /**
     * Standalone/UI-only build: no local server URL. The client restores the
     * last remote connection or shows the login screen.
     */
    @JvmStatic
    fun showStandalone(activity: ComponentActivity) {
        LocalShellHost.spawner = null
        ServerModelHost.writer = null
        activity.setContent { OpenCodeMobileRoot() }
    }

    @JvmStatic
    fun show(activity: ComponentActivity, url: String, password: String) {
        show(activity, url, password, null, null, false, false)
    }

    /**
     * @param shell  本机 Termux shell（null = 终端页只用服务端 PTY）
     * @param fsRoot 文件页的“/”；null = 真机根目录（Androc 用真实文件系统）
     */
    @JvmStatic
    fun show(
        activity: ComponentActivity,
        url: String,
        password: String,
        shell: LocalShellSpawner?,
        fsRoot: String?,
    ) {
        show(activity, url, password, shell, fsRoot, false, false)
    }

    /**
     * @param kernelBundled full 构建为 true；standalone UI 为 false
     * @param toolsBundled  full 构建为 true；thin 构建为 false
     */
    @JvmStatic
    fun show(
        activity: ComponentActivity,
        url: String,
        password: String,
        shell: LocalShellSpawner?,
        fsRoot: String?,
        kernelBundled: Boolean,
        toolsBundled: Boolean,
    ) {
        show(activity, url, password, shell, fsRoot, kernelBundled, toolsBundled, null)
    }

    /**
     * @param plugins optional plugin manager; Androc passes AppPluginManager
     */
    @JvmStatic
    fun show(
        activity: ComponentActivity,
        url: String,
        password: String,
        shell: LocalShellSpawner?,
        fsRoot: String?,
        kernelBundled: Boolean,
        toolsBundled: Boolean,
        plugins: PluginManager?,
    ) {
        LocalShellHost.spawner = shell
        activity.setContent {
            OpenCodeMobileRoot(
                serverUrl = url,
                password = password,
                fsRoot = fsRoot,
                kernelBundled = kernelBundled,
                toolsBundled = toolsBundled,
                plugins = plugins,
            )
        }
    }
}
