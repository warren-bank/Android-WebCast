package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.R;
import com.github.warren_bank.webcast.webview.BrowserUtils;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;

public class BrowserWebViewClient_AdBlock extends BrowserWebViewClient_VideoDetector {
    private boolean isPopulatingHosts;
    private TreeMap<String, Object> blockedHosts;

    private void populateBlockedHosts(Context context) {
        InputStream is = context.getResources().openRawResource(R.raw.adblock_serverlist);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        if (is != null) {
            try {
                blockedHosts = new TreeMap<String, Object>();

                while ((line = br.readLine()) != null) {
                    line = line.toLowerCase().trim();

                    if (!line.isEmpty() && !line.startsWith("#")) {
                        blockedHosts.put(line, null);
                    }
                }
            } catch (IOException e) {
                blockedHosts = null;
            }
        }
    }

    private boolean isHostBlocked(String url) {
        if ((blockedHosts == null) || isPopulatingHosts) return false;

        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost().toLowerCase().trim();
            return blockedHosts.containsKey(host);
        }
        catch(Exception e) {
            return false;
        }
    }

    private WebResourceResponse shouldBlockRequest(String url) {
        if (isHostBlocked(url)) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }
        else {
            return null;
        }
    }

    public BrowserWebViewClient_AdBlock(BrowserActivity browserActivity) {
        super(browserActivity);

        Context context = browserActivity.getApplicationContext();

        if (BrowserUtils.getEnableAdBlockPreference(context)) {
            isPopulatingHosts = true;
            populateBlockedHosts(context);
        }
        isPopulatingHosts = false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        super.shouldInterceptRequest(view, url);

        return shouldBlockRequest(url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        super.shouldInterceptRequest(view, request);

        String url = request.getUrl().toString();
        return shouldBlockRequest(url);
    }
}
