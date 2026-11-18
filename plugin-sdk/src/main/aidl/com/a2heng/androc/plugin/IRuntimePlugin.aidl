// Headless runtime plugin. Implemented by the runtime APK service.
package com.a2heng.androc.plugin;

import com.a2heng.androc.plugin.IRuntimePluginListener;

interface IRuntimePlugin {
    /** JSON: id/name/version/category/skills/toolchain/homepage/source */
    String pluginInfo();

    /** Start the server; returns PluginConnection JSON. May block. */
    String start();

    void stop();

    /** Current PluginConnection JSON. */
    String connection();

    /** One-time pairing link, or empty. */
    String pairingLink();

    void setListener(IRuntimePluginListener listener);
}
