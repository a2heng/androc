package com.a2heng.androc;

import android.content.Context;

import com.a2heng.androc.contracts.PluginInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps skill directories to the plugin that owns them, so uninstalling a
 * plugin removes exactly its skills and nothing else.
 */
final class PluginCatalog {
    private static final String PREF = "app";
    private static final String PREFIX = "plugin_disabled_";

    private static final Map<String, String> SKILL_PLUGIN = new HashMap<>();

    static {
        SKILL_PLUGIN.put("termux", PluginInfo.TERMUX);
        SKILL_PLUGIN.put("skill-installer", "core");
        // Everything else under skills/ (build, AI, debug, install, UI, ...)
        // belongs to the single Android plugin.
    }

    private PluginCatalog() {
    }

    static String pluginFor(String skill) {
        String id = SKILL_PLUGIN.get(skill);
        return id == null ? PluginInfo.ANDROID : id;
    }

    static boolean pluginEnabled(Context ctx, String id) {
        if ("core".equals(id)) return true;
        return !prefs(ctx).getBoolean(PREFIX + id, false);
    }

    static void setPluginEnabled(Context ctx, String id, boolean enabled) {
        prefs(ctx).edit().putBoolean(PREFIX + id, !enabled).apply();
    }

    static boolean skillEnabled(Context ctx, String skill) {
        return pluginEnabled(ctx, pluginFor(skill));
    }

    /** Skill directories currently shipped in the APK for a plugin. */
    static List<String> skillsFor(Context ctx, String id) {
        List<String> out = new ArrayList<>();
        try {
            String[] children = ctx.getAssets().list("skills");
            if (children == null) return out;
            for (String child : children) {
                if (id.equals(pluginFor(child))) out.add(child);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static android.content.SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
