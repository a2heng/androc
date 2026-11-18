package com.a2heng.androc.plugin;

interface IRuntimePluginListener {
    /** stopped / starting / ready / failed */
    void onStateChanged(String state);
}
