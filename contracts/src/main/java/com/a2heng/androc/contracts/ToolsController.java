package com.a2heng.androc.contracts;

/**
 * Optional on-device Android toolchain (skills, device API, JDK/SDK/Gradle).
 *
 * <p>Only the full product embeds it. The kernel must run without it: a
 * missing toolchain degrades the agent's build skills, never the server.</p>
 */
public interface ToolsController {
    /** True once the bundled skills and scripts are installed in HOME. */
    boolean installed();

    /** Installed tools bundle version, or an empty string when unknown. */
    String version();

    /**
     * Installs or repairs the bundle. May be called from the main thread;
     * implementations must do the work off it and report on the main thread.
     */
    void ensureInstalled(Callback callback);

    interface Callback {
        void onToolsResult(boolean ok, String message);
    }
}
