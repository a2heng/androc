package com.a2heng.androc;

import android.content.Context;

import com.a2heng.androc.market.MarketClient;

/**
 * Market endpoint override (developer / mirror support).
 *
 * The default index lives on raw.githubusercontent.com, which is not always
 * reachable from China. `POST /market?url=` on the debug API points the app at
 * a reachable mirror without rebuilding.
 */
final class MarketConfig {
    private static final String PREF = "app";
    private static final String KEY = "market_index_url";

    private MarketConfig() {
    }

    static String indexUrl(Context ctx) {
        String url = ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY, "");
        return url == null || url.isEmpty() ? MarketClient.DEFAULT_INDEX_URL : url;
    }

    static void setIndexUrl(Context ctx, String url) {
        ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, url == null ? "" : url.trim())
                .apply();
    }
}
