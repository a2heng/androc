package com.a2heng.androc.contracts;

/**
 * The UI-facing surface of the on-device opencode kernel.
 *
 * <p>The UI must never start a process, touch the Termux prefix or know about
 * proot. It starts the controller, observes the state and receives a
 * {@link RuntimeConnection}. Every product that embeds a kernel supplies one
 * implementation; the UI-only product supplies none.</p>
 *
 * <p>All methods are safe to call from the main thread unless documented
 * otherwise. Implementations own their threading and must notify listeners on
 * the main thread.</p>
 */
public interface RuntimeController {
    /** Current lifecycle state. */
    RuntimeState state();

    /**
     * Connection for the running server, or {@code null} when
     * {@link #state()} is not {@link RuntimeState#READY}.
     */
    RuntimeConnection connection();

    /**
     * One-time pairing link for the official opencode clients, or {@code null}
     * if the server is not ready. The link expires; ask again for a new one.
     */
    String pairingLink();

    /** Starts toolbox repair (if needed) and the server. Idempotent. */
    void start();

    /** Stops the server and releases its foreground service. Idempotent. */
    void stop();

    void addListener(Listener listener);

    void removeListener(Listener listener);

    interface Listener {
        /** Called on the main thread whenever {@link #state()} changes. */
        void onRuntimeStateChanged(RuntimeState state);

        /**
         * Called on the main thread once a new connection is ready. A listener
         * that was added while already ready receives it immediately.
         */
        default void onRuntimeConnected(RuntimeConnection connection) {
        }
    }
}
