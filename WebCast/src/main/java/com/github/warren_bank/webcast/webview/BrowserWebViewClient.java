package com.github.warren_bank.webcast.webview;

import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.CookieManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import okhttp3.HttpUrl;
import okhttp3.Cookie;
import okhttp3.CookieJar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BrowserWebViewClient extends WebViewClient {

    /**
     * source:  https://gist.github.com/Jthomas54/2f34b8aea5b457db5459e2421deffd15
     * license: undefined
     *
     * Provides a synchronization point between the webview cookie store and okhttp3.OkHttpClient cookie store
     */
    public final class WebviewCookieHandler implements CookieJar {
        private CookieManager webviewCookieManager = CookieManager.getInstance();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            String urlString = url.toString();

            for (Cookie cookie : cookies) {
                webviewCookieManager.setCookie(urlString, cookie.toString());
            }
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            String urlString = url.toString();
            String cookiesString = webviewCookieManager.getCookie(urlString);

            if (cookiesString != null && !cookiesString.isEmpty()) {
                //We can split on the ';' char as the cookie manager only returns cookies
                //that match the url and haven't expired, so the cookie attributes aren't included
                String[] cookieHeaders = cookiesString.split(";");
                List<Cookie> cookies = new ArrayList<>(cookieHeaders.length);

                for (String header : cookieHeaders) {
                    Cookie c = Cookie.parse(url, header);
                    if(c != null) {
                      cookies.add(c);
                    }
                }

                return cookies;
            }

            return Collections.emptyList();
        }
    }

    private BrowserActivity browserActivity;

    private WebviewCookieHandler cookieJar;
    private OkHttpClient httpClient;

    public BrowserWebViewClient(BrowserActivity browserActivity) {
        super();

        this.browserActivity = browserActivity;

        this.cookieJar       = new WebviewCookieHandler();
        this.httpClient      = new OkHttpClient.Builder().cookieJar(this.cookieJar).build();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }

    private WebResourceResponse okHttpResponseToWebResourceResponse(Response response) {
        final ResponseBody body   = response.body();
        final MediaType mediaType = body.contentType();

        final Charset _charset    = mediaType.charset();
        final String _ctype_major = mediaType.type();
        final String _ctype_minor = mediaType.subtype();

        final String charset      = (_charset == null) ? null : _charset.name();
        String contentType;

        if ((_ctype_major == null) || (_ctype_major.length() == 0)) {
            contentType = "application/octet-stream";
        }
        else {
            if ((_ctype_minor == null) || (_ctype_minor.length() == 0)) {
                contentType = _ctype_major;
            }
            else {
                contentType = _ctype_major + "/" + _ctype_minor;
            }
        }

        return new WebResourceResponse(contentType, charset, body.byteStream());
    }

    private WebResourceResponse shouldInterceptRequest(WebView view, String url, Map<String, String> reqHeaders) {
        try {
            final Request.Builder builder = new Request.Builder();
            builder.url(url);

            if ((reqHeaders != null) && (reqHeaders.size() > 0)) {
                for (Map.Entry<String,String> header : reqHeaders.entrySet()) {
                    builder.addHeader(
                        header.getKey(),
                        header.getValue()
                    );
                }
            }

            final Request request   = builder.build();
            final Response response = this.httpClient.newCall(request).execute();

            return okHttpResponseToWebResourceResponse(response);
        }
        catch (IOException e) {
            // try again
            return null;
        }
        catch (Exception e) {
            // try again
            return null;
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Map<String, String> reqHeaders = new HashMap<String,String>();

        reqHeaders.put(
            "User-Agent",
            view.getSettings().getUserAgentString()
        );
        reqHeaders.put(
            "Referer",
            url
        );

        return shouldInterceptRequest(view, url, reqHeaders);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url                     = request.getUrl().toString();
        Map<String, String> reqHeaders = request.getRequestHeaders();

        return shouldInterceptRequest(view, url, reqHeaders);
    }
}
