package com.a2heng.androc.ui.data

/**
 * 当前 APK 的发行版，由 app 模块在启动时写入。
 *
 * thin = 只有 UI；bundle = UI + runtime + OpenCode。
 * 自更新必须下载同一发行版的 APK，否则 thin 会拉一个 160MB 的 bundle。
 */
object AppBuild {
    @Volatile
    var thin: Boolean = true
}
