package com.a2heng.androc.contracts;

import java.util.Collections;
import java.util.List;

/**
 * The standard opencode pairing contract.
 *
 * <p>This is what makes the kernel interoperable with the official Android
 * app and with every other opencode client. We do not invent a discovery
 * protocol: the server issues a one-time code, the client opens
 * {@code <baseUrl>/auth/connect/<code>} and receives a session token.</p>
 */
public final class Pairing {
    /** One-time codes are valid for 5 minutes on the server. */
    public static final long CODE_TTL_SECONDS = 5 * 60;

    private Pairing() {
    }

    /** {@code <baseUrl>/auth/connect/<code>} without duplicate slashes. */
    public static String link(String baseUrl, String code) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/auth/connect/" + code;
    }

    /**
     * JSON payload embedded in the pairing QR code for reachable server URLs:
     * {@code {"code":"...","urls":["http://..."]}}.
     */
    public static String qrPayload(String code, List<String> urls) {
        StringBuilder out = new StringBuilder(64);
        out.append("{\"code\":\"").append(escape(code)).append("\",\"urls\":[");
        if (urls != null) {
            for (int i = 0; i < urls.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append('"').append(escape(urls.get(i))).append('"');
            }
        }
        return out.append("]}").toString();
    }

    public static String qrPayload(String code, String url) {
        return qrPayload(code, url == null
                ? Collections.<String>emptyList()
                : Collections.singletonList(url));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }
}
