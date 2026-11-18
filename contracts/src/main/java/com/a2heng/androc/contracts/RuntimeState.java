package com.a2heng.androc.contracts;

/** Lifecycle state of the on-device opencode kernel. */
public enum RuntimeState {
    /** No server process; nothing is listening. */
    STOPPED,
    /** Toolbox repair or server startup is in progress. */
    STARTING,
    /** The HTTP API answers and {@link RuntimeController#connection()} is usable. */
    READY,
    /** Startup failed; the reason is in the kernel log. */
    FAILED;

    public boolean isRunning() {
        return this == STARTING || this == READY;
    }

    public boolean isReady() {
        return this == READY;
    }
}
