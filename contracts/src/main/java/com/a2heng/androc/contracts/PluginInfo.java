package com.a2heng.androc.contracts;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One installable plugin: a skill (or skill group) plus the toolchain it needs.
 *
 * <p>The base UI is not a plugin. OpenCode itself is a plugin, so the app can
 * start with only the proot engine and load the runtime later.</p>
 */
public final class PluginInfo {
    public static final String OPENCODE = "opencode";
    public static final String TERMUX = "termux";
    /** One Android plugin: every Android skill + the whole Android toolchain. */
    public static final String ANDROID = "android";

    private final String id;
    private final String name;
    private final String summary;
    private final List<String> skills;
    private final List<String> toolchain;
    private final String version;
    private final String latestVersion;
    private final String sizeText;
    private final boolean installed;
    private final boolean bundled;
    private final boolean removable;
    private final boolean updateAvailable;

    public PluginInfo(String id, String name, String summary,
                      List<String> skills, List<String> toolchain,
                      String version, String latestVersion, String sizeText,
                      boolean installed, boolean bundled, boolean removable,
                      boolean updateAvailable) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = name == null ? id : name;
        this.summary = summary == null ? "" : summary;
        this.skills = immutable(skills);
        this.toolchain = immutable(toolchain);
        this.version = version == null ? "" : version;
        this.latestVersion = latestVersion == null ? "" : latestVersion;
        this.sizeText = sizeText == null ? "" : sizeText;
        this.installed = installed;
        this.bundled = bundled;
        this.removable = removable;
        this.updateAvailable = updateAvailable;
    }

    private static List<String> immutable(List<String> value) {
        return value == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(value);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** One-line explanation of what this plugin enables. */
    public String summary() {
        return summary;
    }

    public List<String> skills() {
        return skills;
    }

    public List<String> toolchain() {
        return toolchain;
    }

    /** Installed version, or the bundled version when not installed yet. */
    public String version() {
        return version;
    }

    /** Newest known version (upstream for OpenCode, pack manifest for tools). */
    public String latestVersion() {
        return latestVersion;
    }

    public String sizeText() {
        return sizeText;
    }

    public boolean installed() {
        return installed;
    }

    public boolean bundled() {
        return bundled;
    }

    public boolean removable() {
        return removable;
    }

    public boolean updateAvailable() {
        return updateAvailable;
    }
}
