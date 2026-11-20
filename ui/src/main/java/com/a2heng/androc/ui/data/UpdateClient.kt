package com.a2heng.androc.ui.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 从 GitHub Releases 检查/下载更新（公开仓库，不需要 token）。 */
object UpdateClient {

    private const val API = "https://api.github.com/repos/a2heng/androc/releases/latest"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 强制 HTTP/1.1：手机网络下 GitHub 的 CDN / 运营商中间设备经常直接回
     * 非 HTTP/2 内容，OkHttp 会抛 "Required SETTINGS preface not received"。
     * HTTP/1.1 在所有链路上都稳定，APK 下载不需要多路复用。
     */
    private val http = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun latest(): UpdateInfo? {
        var last: Exception? = null
        for (attempt in 0 until 3) {
            try {
                return latestOnce()
            } catch (e: IOException) {
                last = e
                if (attempt < 2) Thread.sleep(1200L * (attempt + 1))
            }
        }
        throw last ?: IOException("检查更新失败")
    }

    private fun latestOnce(): UpdateInfo? {
        val request = Request.Builder()
            .url(API)
            .header("Accept", "application/vnd.github+json")
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val tag = (body["tag_name"] as? JsonPrimitive)?.content ?: return null
            val page = (body["html_url"] as? JsonPrimitive)?.content ?: return null
            val assets = body["assets"]?.jsonArray ?: return UpdateInfo(tag, page, null)
            // 自更新必须跟随发行版：thin 只拿 thin.apk，bundle 只拿 bundle.apk。
            // 旧的 full.apk 只作为 bundle 的兜底。
            val suffix = if (AppBuild.thin) "-thin.apk" else "-bundle.apk"
            val candidates = assets.mapNotNull { it as? JsonObject }
                .filter { ((it["name"] as? JsonPrimitive)?.content ?: "").endsWith(".apk") }
            val apk = (candidates.firstOrNull {
                ((it["name"] as? JsonPrimitive)?.content ?: "").endsWith(suffix)
            } ?: if (AppBuild.thin) null else candidates.firstOrNull {
                ((it["name"] as? JsonPrimitive)?.content ?: "").endsWith("-full.apk")
            })?.let { (it["browser_download_url"] as? JsonPrimitive)?.content }
            return UpdateInfo(tag, page, apk)
        }
    }

    /**
     * 下载 APK：先写 `*.part`，用 Range 续传，完整后原子改名。
     * 网络中断时保留已下载部分，最多重试 3 次，不再从 0 开始。
     */
    fun download(url: String, dest: File, onProgress: (Int) -> Unit) {
        dest.parentFile?.mkdirs()
        val part = File(dest.parentFile, dest.name + ".part")
        var last: Exception? = null
        for (attempt in 0 until 3) {
            try {
                if (downloadOnce(url, part, onProgress)) {
                    if (dest.exists()) dest.delete()
                    if (!part.renameTo(dest)) {
                        part.copyTo(dest, overwrite = true)
                        part.delete()
                    }
                    return
                }
            } catch (e: Exception) {
                last = e
            }
            if (attempt < 2) Thread.sleep(1200L * (attempt + 1))
        }
        throw last ?: IOException("下载失败")
    }

    /** @return true 表示已完整下载；false 表示本地残片不可用，已删除、可重来。 */
    private fun downloadOnce(url: String, part: File, onProgress: (Int) -> Unit): Boolean {
        var offset = if (part.isFile) part.length() else 0L
        val request = Request.Builder().url(url).apply {
            if (offset > 0) header("Range", "bytes=$offset-")
        }.build()
        http.newCall(request).execute().use { resp ->
            if (resp.code == 416) {
                part.delete()
                return false
            }
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val resuming = offset > 0 && resp.code == 206
            if (!resuming) offset = 0
            val body = resp.body ?: throw IOException("空响应")
            val length = body.contentLength()
            val total = if (length > 0) length + offset else -1L
            FileOutputStream(part, resuming).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var read = offset
                    var lastPercent = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        out.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    if (total > 0 && read != total) {
                        throw IOException("连接中断 $read/$total")
                    }
                }
            }
        }
        return true
    }

    /** local 0.1.3 / remote v0.1.4 → true */
    fun isNewer(local: String, remoteTag: String): Boolean {
        fun parse(v: String): List<Int>? =
            v.removePrefix("v").substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }
        val a = parse(local) ?: return false
        val b = parse(remoteTag) ?: return false
        for (i in 0..2) {
            if (b[i] != a[i]) return b[i] > a[i]
        }
        return false
    }
}
