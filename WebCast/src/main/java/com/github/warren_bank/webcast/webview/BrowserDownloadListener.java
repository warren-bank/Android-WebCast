package com.github.warren_bank.webcast.webview;

import android.webkit.DownloadListener;

public class BrowserDownloadListener implements DownloadListener {

    private BrowserWebViewClient client;

    public BrowserDownloadListener(BrowserWebViewClient client) {
        this.client = client;
    }

    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        client.process_URL(url);
    }
}
