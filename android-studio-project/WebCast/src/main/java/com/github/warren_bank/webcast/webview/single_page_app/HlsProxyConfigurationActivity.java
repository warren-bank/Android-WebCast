package com.github.warren_bank.webcast.webview.single_page_app;

import com.github.warren_bank.webcast.R;

import android.content.Context;

public class HlsProxyConfigurationActivity extends AbstractWebcastReloadedSinglePageAppActivity {
    @Override
    protected void init() {
        prevent_leaving_page       = false;
        page_url_base              = get_page_url_base(HlsProxyConfigurationActivity.this);
        pref_persistentcookies_key = getString(R.string.pref_hlsproxyconfiguration_persistentcookies_key);

        initWebcastReloaded();
    }

    public static String get_page_url_base(Context context) {
        return context.getString(R.string.url_hlsproxyconfiguration);
    }
}
