package com.a2heng.androc.market

import org.json.JSONObject

/** Parsed, signature-verified market index. */
data class MarketIndex(
    val generated: String,
    val opencodeVersion: String,
    val packs: List<MarketPack>,
) {
    fun find(id: String): MarketPack? = packs.firstOrNull { it.id == id }

    companion object {
        fun parse(root: JSONObject): MarketIndex {
            val packs = buildList {
                root.optJSONArray("packs")?.let { array ->
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.let { add(MarketPack.parse(it)) }
                    }
                }
            }
            return MarketIndex(
                generated = root.optString("generated", ""),
                opencodeVersion = root.optJSONObject("upstream")?.optString("opencode", "").orEmpty(),
                packs = packs,
            )
        }
    }
}

data class MarketPack(
    val id: String,
    val name: String,
    val version: String,
    val upstreamVersion: String,
    val abi: String,
    val installedSize: Long,
    val installedSha: String,
    val archiveUrl: String,
    val archiveSize: Long,
    val archiveSha: String,
    val delta: MarketDelta?,
) {
    companion object {
        fun parse(o: JSONObject): MarketPack {
            val installed = o.optJSONObject("installed")
            val archive = o.optJSONObject("archive")
            val d = o.optJSONObject("delta")
            return MarketPack(
                id = o.optString("id", ""),
                name = o.optString("name", ""),
                version = o.optString("version", ""),
                upstreamVersion = o.optString("upstreamVersion", ""),
                abi = o.optString("abi", "arm64-v8a"),
                installedSize = installed?.optLong("size", 0) ?: 0,
                installedSha = installed?.optString("sha256", "").orEmpty(),
                archiveUrl = archive?.optString("url", "").orEmpty(),
                archiveSize = archive?.optLong("size", 0) ?: 0,
                archiveSha = archive?.optString("sha256", "").orEmpty(),
                delta = d?.let { MarketDelta.parse(it) },
            )
        }
    }
}

data class MarketDelta(
    val from: String,
    val fromSha: String,
    val url: String,
    val size: Long,
    val sha: String,
    val windowLog: Int,
) {
    companion object {
        fun parse(o: JSONObject): MarketDelta = MarketDelta(
            from = o.optString("from", ""),
            fromSha = o.optString("fromSha256", ""),
            url = o.optString("url", ""),
            size = o.optLong("size", 0),
            sha = o.optString("sha256", ""),
            windowLog = o.optInt("long", 27),
        )
    }
}
