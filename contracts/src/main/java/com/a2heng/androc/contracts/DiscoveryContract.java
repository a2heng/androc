package com.a2heng.androc.contracts;

/**
 * Optional local discovery provider.
 *
 * <p>A kernel may export a read-only {@code content://<applicationId>.local-server/info}
 * provider so another app on the same phone can discover the loopback URL and
 * password without copy/paste. This is a convenience only: it is <b>not</b> the
 * interoperability contract. The official opencode app and other clients use
 * the standard HTTP API plus {@link Pairing}.</p>
 */
public final class DiscoveryContract {
    public static final String AUTHORITY_SUFFIX = ".local-server";
    public static final String PATH_INFO = "info";
    public static final String PERMISSION_SUFFIX = ".permission.LOCAL_SERVER_INFO";
    public static final String COLUMN_INFO = "info";

    private DiscoveryContract() {
    }

    public static String authority(String applicationId) {
        return applicationId + AUTHORITY_SUFFIX;
    }

    public static String permission(String applicationId) {
        return applicationId + PERMISSION_SUFFIX;
    }

    public static String uri(String applicationId) {
        return "content://" + authority(applicationId) + "/" + PATH_INFO;
    }
}
