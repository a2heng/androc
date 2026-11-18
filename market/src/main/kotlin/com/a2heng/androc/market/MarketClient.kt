package com.a2heng.androc.market

import android.util.Base64
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Signed market client: index verification, download, install.
 *
 * Only the public key is compiled in. The index signature covers the exact
 * bytes of index.json; a valid signature is required before parsing. Packs are
 * verified by SHA-256 again before they become visible.
 *
 * Decompression is pure Java XZ streaming: no native zstd, no Unsafe, no
 * per-device ABI surprises.
 */
class MarketClient(
    private val indexUrl: String = DEFAULT_INDEX_URL,
    publicKeyBase64: String = DEFAULT_PUBLIC_KEY,
) {
    fun interface Progress {
        fun onProgress(done: Long, total: Long)
    }

    private val publicKey: ByteArray = Base64.decode(publicKeyBase64, Base64.DEFAULT)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.MINUTES)
        .build()

    fun refresh(): MarketIndex {
        val index = fetch(indexUrl)
        val signature = fetch("$indexUrl.sig")
        verify(index, signature)
        return MarketIndex.parse(JSONObject(String(index, Charsets.UTF_8)))
    }

    /**
     * Install kernel-opencode into files/runtime/<upstream>/opencode.
     * Full pack only for now; the previous binary stays until the new one is verified.
     */
    fun installOpencode(filesDir: File, progress: Progress? = null): File {
        val index = refresh()
        val pack = index.find("kernel-opencode") ?: throw IOException("kernel-opencode not in index")
        if (pack.abi != "arm64-v8a") throw IOException("unsupported abi ${pack.abi}")

        val cache = File(filesDir, "market").apply { if (!isDirectory && !mkdirs()) throw IOException("cannot create cache") }
        val targetDir = File(File(filesDir, "runtime"), pack.upstreamVersion)
        if (!targetDir.isDirectory && !targetDir.mkdirs()) throw IOException("cannot create runtime dir")
        val target = File(targetDir, "opencode")
        val tmp = File(targetDir, "opencode.new")

        val archive = File(cache, "opencode-${pack.version}.xz")
        download(pack.archiveUrl, archive, pack.archiveSize, pack.archiveSha, progress)
        decompress(archive, tmp)

        if (tmp.length() != pack.installedSize || !sha256(tmp).equals(pack.installedSha, ignoreCase = true)) {
            tmp.delete()
            throw IOException("installed hash mismatch")
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        target.setExecutable(true, true)

        cache.listFiles()?.forEach { f ->
            if (f.isFile && f.name != archive.name) f.delete()
        }
        return target
    }

    // ------------------------------------------------------------------ crypto

    private fun verify(payload: ByteArray, signatureText: ByteArray) {
        val sig = Base64.decode(String(signatureText, Charsets.UTF_8).trim(), Base64.DEFAULT)
        val spec = EdDSAPublicKeySpec(publicKey, EdDSANamedCurveTable.getByName("Ed25519"))
        val key = EdDSAPublicKey(spec)
        val engine = EdDSAEngine()
        engine.initVerify(key)
        engine.update(payload)
        if (!engine.verify(sig)) throw SecurityException("market index signature rejected")
    }

    // ----------------------------------------------------------------- network

    private fun fetch(url: String): ByteArray {
        val request = Request.Builder().url(url).header("User-Agent", "androc-market/1").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} $url")
            return response.body?.bytes() ?: throw IOException("empty body $url")
        }
    }

    private fun download(url: String, dest: File, expectedSize: Long, expectedSha: String, progress: Progress?) {
        if (dest.isFile && dest.length() == expectedSize && sha256(dest).equals(expectedSha, true)) {
            progress?.onProgress(expectedSize, expectedSize)
            return
        }
        val part = File(dest.parentFile, "${dest.name}.part")
        val request = Request.Builder().url(url).header("User-Agent", "androc-market/1").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} $url")
            val body = response.body ?: throw IOException("empty body $url")
            val total = if (body.contentLength() > 0) body.contentLength() else expectedSize
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            body.byteStream().use { input ->
                BufferedOutputStream(FileOutputStream(part), 1 shl 16).use { output ->
                    val buf = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        done += n
                        progress?.onProgress(done, total)
                    }
                }
            }
            val sha = digest.digest().toHex()
            if (done != expectedSize || !sha.equals(expectedSha, true)) {
                part.delete()
                throw IOException("download hash mismatch $sha")
            }
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) throw IOException("cannot rename $dest")
        }
    }

    // -------------------------------------------------------------------- xz

    private fun decompress(archive: File, out: File) {
        BufferedInputStream(FileInputStream(archive), 1 shl 16).use { input ->
            XZInputStream(input).use { xz ->
                BufferedOutputStream(FileOutputStream(out), 1 shl 16).use { output ->
                    val buf = ByteArray(1 shl 16)
                    while (true) {
                        val n = xz.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    companion object {
        const val DEFAULT_INDEX_URL = "https://raw.githubusercontent.com/a2heng/androc-market/main/index.json"

        /** Raw Ed25519 public key (32 bytes), base64. */
        const val DEFAULT_PUBLIC_KEY = "g7CH9PNh60W57Kgn5twCtpl68wpfxFdSAlBzn0ao7Uw="

        fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            BufferedInputStream(FileInputStream(file), 1 shl 16).use { input ->
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
            return md.digest().toHex()
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }
}
