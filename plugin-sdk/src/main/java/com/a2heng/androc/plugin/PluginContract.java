package com.a2heng.androc.plugin;

import com.a2heng.androc.contracts.RuntimeConnection;

/** Shared names for the headless plugin APK contract. */
public final class PluginContract {
    /** Service action every plugin declares. */
    public static final String ACTION_BIND = "com.a2heng.androc.plugin.BIND";
    /** Signature-level permission; only plugins signed with the same key can bind. */
    public static final String PERMISSION = "com.a2heng.androc.permission.PLUGIN";
    /** Metadata asset inside the plugin APK. */
    public static final String ASSET_MANIFEST = "plugin.json";
    /** Category ids. */
    public static final String CATEGORY_CORE = "core";
    public static final String CATEGORY_RUNTIME = "runtime";
    public static final String CATEGORY_ANDROID = "android";
    public static final String CATEGORY_OTHER = "other";

    private PluginContract() {
    }
}
