package com.a2heng.androc.ui.data

/**
 * 把服务端/网络层的原始报错翻成用户能看懂的一句话。
 *
 * OpenCode 的报错经常是 `Can't reach api.deepseek.com. Check the network connection.`，
 * 但真实原因可能是 DNS、超时、Key 或余额。这里做最小映射，认不出的原文返回。
 */
fun friendlyError(raw: String?): String {
    if (raw.isNullOrBlank()) return "请求失败"
    val s = raw.lowercase()
    return when {
        s.contains("eai_again") || s.contains("enotfound") ||
            s.contains("getaddrinfo") || s.contains("name or service not known") ||
            s.contains("nodename nor servname") || s.contains("temporary failure in name resolution") ->
            "域名解析失败（DNS），请检查网络或 DNS 设置"

        s.contains("401") || s.contains("unauthorized") ||
            s.contains("invalid api key") || s.contains("incorrect api key") ||
            s.contains("authentication") ->
            "API Key 无效或已过期"

        s.contains("402") || s.contains("429") || s.contains("quota") ||
            s.contains("rate limit") || s.contains("insufficient") ||
            s.contains("balance") ->
            "余额不足或触发限流"

        s.contains("can't reach") || s.contains("cannot reach") ->
            "无法连接服务器：先检查 DNS，其次检查网络是否被拦截"

        s.contains("etimedout") || s.contains("econnrefused") ||
            s.contains("econnreset") || s.contains("connection refused") ||
            s.contains("connection timed out") || s.contains("connect timeout") ->
            "连接超时，可能被网络拦截"

        else -> raw
    }
}
