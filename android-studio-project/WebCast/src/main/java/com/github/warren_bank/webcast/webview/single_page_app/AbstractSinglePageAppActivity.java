package com.github.warren_bank.webcast.webview.single_page_app;

import com.github.warren_bank.webcast.R;
import com.github.warren_bank.webcast.WebCastApplication;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import androidx.appcompat.app.AppCompatActivity;

public abstract class AbstractSinglePageAppActivity extends AppCompatActivity {
    protected boolean prevent_leaving_page;
    protected String  page_url_base;
    protected String  page_url_domain;
    protected String  page_url_path;
    protected String  page_url_href;
    protected String  pref_persistentcookies_key;
    private WebView   webView;

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Events:
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prevent_leaving_page = true;

        init();
        if (page_url_href == null)
            finish();

        setContentView(R.layout.single_page_app_activity);
        webView = (WebView)findViewById(R.id.webView);
        initWebView();
    }

    @Override
    public void onStart() {
        super.onStart();

        restoreCookies();
        webView.loadUrl(page_url_href);
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
    // Abstract:
    // ---------------------------------------------------------------------------------------------

    protected abstract void init();

    // ---------------------------------------------------------------------------------------------
    // WebView:
    // ---------------------------------------------------------------------------------------------

    private void initWebView() {
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

        if (prevent_leaving_page) {
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
        }
    }

    private void restoreCookies() {
        if (
            (pref_persistentcookies_key == null)
         || (page_url_base              == null)
         || (page_url_domain            == null)
         || (page_url_path              == null)
        ) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String cookies          = prefs.getString(pref_persistentcookies_key, null);

        if (cookies != null) {
            String[] cookiesArr = cookies.split("\\s*;\\s+");
            CookieManager CM    = CookieManager.getInstance();
            String cookie;

            for (int i=0; i < cookiesArr.length; i++) {
                cookie = cookiesArr[i] + "; Domain=" + page_url_domain + "; Path=" + page_url_path;
                CM.setCookie(page_url_base, cookie);
            }
        }
    }

    private void saveCookies() {
        if (
            (pref_persistentcookies_key == null)
         || (page_url_base              == null)
        ) return;

        String cookies = CookieManager.getInstance().getCookie(page_url_base);

        if (cookies != null) {
            SharedPreferences prefs         = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString(pref_persistentcookies_key, cookies);
            editor.commit();
        }
    }

}
