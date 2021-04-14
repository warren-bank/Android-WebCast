package com.github.warren_bank.webcast.webview.single_page_app;

import com.github.warren_bank.webcast.webview.BrowserUtils;

import android.content.Intent;
import android.net.Uri;

public abstract class AbstractWebcastReloadedSinglePageAppActivity extends AbstractSinglePageAppActivity {
    protected void initWebcastReloaded() {
        if (page_url_base == null) return;

        Uri page        = Uri.parse(page_url_base);
        page_url_domain = page.getHost();
        page_url_path   = page.getPath();
        page_url_href   = get_page_url_href(page_url_base, getIntent());
    }

    public static String get_page_url_href(String page_url_base, Intent intent) {
        String video    = intent.getDataString();
        String caption  = intent.getStringExtra("textUrl");
        String referer  = intent.getStringExtra("referUrl");

        return (video == null)
            ? null
            : (
                page_url_base                                +
                "#/watch/"                                   +
                BrowserUtils.base64_encode(video)            +
                ((caption == null)
                    ? ""
                    : (
                        "/subtitle/"                         +
                        BrowserUtils.base64_encode(caption)
                      )
                )                                            +
                ((referer == null)
                    ? ""
                    : (
                        "/referer/"                          +
                        BrowserUtils.base64_encode(referer)
                      )
                )
              )
        ;
    }
}
