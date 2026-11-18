package com.a2heng.androc.contracts;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Immutable description of one opencode server endpoint.
 *
 * <p>This is the only connection shape passed between the kernel and the UI.
 * It deliberately mirrors the standard opencode HTTP contract: basic auth with
 * the fixed username {@value #DEFAULT_USERNAME} plus the server password.
 * Nothing here is Androc-specific, so the same value can describe a server on
 * the phone, a computer on the LAN, or a tunnel.</p>
 */
public final class RuntimeConnection {
    /** opencode's basic-auth username is always "opencode". */
    public static final String DEFAULT_USERNAME = "opencode";

    private final String baseUrl;
    private final String username;
    private final String password;

    public RuntimeConnection(String baseUrl, String username, String password) {
        String url = Objects.requireNonNull(baseUrl, "baseUrl").trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException("baseUrl is empty");
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.baseUrl = url;
        this.username = username == null || username.isEmpty() ? DEFAULT_USERNAME : username;
        this.password = password == null ? "" : password;
    }

    /** Standard opencode connection: {@code http://host:port} + password. */
    public static RuntimeConnection basic(String baseUrl, String password) {
        return new RuntimeConnection(baseUrl, DEFAULT_USERNAME, password);
    }

    /** Base URL without a trailing slash, for example {@code http://127.0.0.1:4096}. */
    public String baseUrl() {
        return baseUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public boolean hasPassword() {
        return !password.isEmpty();
    }

    /** Value for the {@code Authorization} request header. */
    public String authorizationHeader() {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * One-time pairing link accepted by standard opencode clients.
     * The server consumes the code and issues a 30-day session token.
     */
    public String pairingLink(String code) {
        return Pairing.link(baseUrl, code);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuntimeConnection)) {
            return false;
        }
        RuntimeConnection that = (RuntimeConnection) other;
        return baseUrl.equals(that.baseUrl)
                && username.equals(that.username)
                && password.equals(that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, username, password);
    }

    @Override
    public String toString() {
        return "RuntimeConnection{" + baseUrl + ", user=" + username
                + ", password=" + (password.isEmpty() ? "<none>" : "<redacted>") + "}";
    }
}
