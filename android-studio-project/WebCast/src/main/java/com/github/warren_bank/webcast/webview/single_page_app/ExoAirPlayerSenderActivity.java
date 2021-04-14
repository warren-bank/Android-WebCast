package com.github.warren_bank.webcast.webview.single_page_app;

import com.github.warren_bank.webcast.R;

import android.content.Context;
import android.os.Build;

public class ExoAirPlayerSenderActivity extends AbstractWebcastReloadedSinglePageAppActivity {
    @Override
    protected void init() {
        prevent_leaving_page       = true;
        page_url_base              = get_page_url_base(ExoAirPlayerSenderActivity.this);
        pref_persistentcookies_key = getString(R.string.pref_exoairplayersender_persistentcookies_key);

        initWebcastReloaded();
    }

    public static String get_page_url_base(Context context) {
        // 'Android System WebView' can be updated in Android 5.0+
        return (Build.VERSION.SDK_INT >= 21)
            ? context.getString(R.string.url_exoairplayersender_es6)
            : context.getString(R.string.url_exoairplayersender_es5)
        ;
    }
}
