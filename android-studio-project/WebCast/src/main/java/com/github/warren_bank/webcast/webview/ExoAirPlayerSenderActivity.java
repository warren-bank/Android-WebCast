package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.R;
import com.github.warren_bank.webcast.WebCastApplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import androidx.appcompat.app.AppCompatActivity;

public class ExoAirPlayerSenderActivity extends AppCompatActivity {
    private String  AIRPLAY_SENDER;
    private String  page_domain;
    private String  page_path;
    private String  page_url;
    private WebView webView;

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Events:
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPageUrl();
        if (page_url == null)
            finish();

        setContentView(R.layout.exoairplayer_sender_activity);
        webView = (WebView)findViewById(R.id.webView);
        initWebView();
    }

    @Override
    public void onStart() {
        super.onStart();

        restoreCookies();
        webView.loadUrl(page_url);
    }

    @Override
    public void onResume() {
        super.onResume();
        WebCastApplication.activityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        WebCastApplication.activityPaused();
    }

    @Override
    public void onStop() {
        super.onStop();

        saveCookies();
    }

    // ---------------------------------------------------------------------------------------------
    // Intent:
    // ---------------------------------------------------------------------------------------------

    private void getPageUrl() {
        // 'Android System WebView' can be updated in Android 5.0+
        AIRPLAY_SENDER = (Build.VERSION.SDK_INT >= 21)
            ? getString(R.string.url_airplay_sender_es6)
            : getString(R.string.url_airplay_sender_es5)
        ;

        Uri page       = Uri.parse(AIRPLAY_SENDER);
        Intent intent  = getIntent();
        String video   = intent.getDataString();
        String caption = intent.getStringExtra("textUrl");
        String referer = intent.getStringExtra("referUrl");

        page_domain    = page.getHost();
        page_path      = page.getPath();
        page_url       = (video == null)
            ? null
            : (
                AIRPLAY_SENDER                               +
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

    // ---------------------------------------------------------------------------------------------
    // WebView:
    // ---------------------------------------------------------------------------------------------

    private void initWebView() {
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(false);
        webSettings.setUserAgentString(
            getResources().getString(R.string.user_agent)
        );
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setInitialScale(0);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
    }

    private void restoreCookies() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String pref_key         = getString(R.string.pref_persistentcookies_key);
        String cookies          = prefs.getString(pref_key, null);

        if (cookies != null) {
            String[] cookiesArr = cookies.split("\\s*;\\s+");
            CookieManager CM    = CookieManager.getInstance();
            String cookie;

            for (int i=0; i < cookiesArr.length; i++) {
                cookie = cookiesArr[i] + "; Domain=" + page_domain + "; Path=" + page_path;
                CM.setCookie(AIRPLAY_SENDER, cookie);
            }
        }
    }

    private void saveCookies() {
        String cookies = CookieManager.getInstance().getCookie(AIRPLAY_SENDER);

        if (cookies != null) {
            SharedPreferences prefs         = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            String pref_key                 = getString(R.string.pref_persistentcookies_key);

            editor.putString(pref_key, cookies);
            editor.commit();
        }
    }

}
