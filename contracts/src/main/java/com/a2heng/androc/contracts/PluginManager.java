package com.a2heng.androc.contracts;

import java.util.List;

/**
 * Install / update / uninstall plugins and check their independent updates.
 *
 * <p>Implementations own storage and threading. Results are delivered on the
 * main thread. Uninstalling a plugin removes only its skills and toolchain;
 * projects and sessions are never touched.</p>
 */
public interface PluginManager {
    /** Current snapshot; safe to call from the main thread. */
    List<PluginInfo> list();

    /** Re-read installed versions from disk. */
    void refresh();

    /** Check the upstream/pack source for one plugin, then refresh. */
    void checkUpdates(String id, Callback callback);

    /** Install or reinstall {@code id}. */
    void install(String id, Callback callback);

    /** Install the newest known version (same as install today). */
    void update(String id, Callback callback);

    /** Remove {@code id}'s skills and toolchain. */
    void uninstall(String id, Callback callback);

    /** Install from a local file (SAF URI). {@code uri} is a string URI. */
    void importFrom(String uri, Callback callback);

    interface Callback {
        void onPluginResult(String id, boolean ok, String message);
    }
}
