package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.SharedUtils;

import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

public class BrowserWebViewClient extends WebViewClient {

    private BrowserActivity browserActivity;

    private void process_URL(String uri, WebView view) {
        String mimeType = SharedUtils.getVideoMimeType(uri);

        if (mimeType != null) {
            browserActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String referer = (view == null) ? null : view.getUrl();
                    browserActivity.addSavedVideo(uri, mimeType, referer);
                }
            });
        }
    }

    protected void process_URL(String uri) {
        process_URL(uri, null);
    }

    public BrowserWebViewClient(BrowserActivity browserActivity) {
        super();

        this.browserActivity = browserActivity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        process_URL(url, view);
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        process_URL(url, view);
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        process_URL(url, view);
        return null;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        process_URL(url, view);
        return null;
    }
}
