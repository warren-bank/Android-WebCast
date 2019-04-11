package com.github.warren_bank.webcast.webview;

import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class BrowserWebViewClient extends WebViewClient {
    private BrowserActivity browserActivity;

    private HttpClient httpClient;
    private HttpClientContext clientContext;
    private CookieStore cookieStore;

    public BrowserWebViewClient(BrowserActivity browserActivity) {
        super();

        this.browserActivity = browserActivity;

        this.httpClient      = HttpClientBuilder.create().build();
        this.clientContext   = HttpClientContext.create();
        this.cookieStore     = new BasicCookieStore();

        this.clientContext.setCookieStore(this.cookieStore);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }

    private WebResourceResponse shouldInterceptRequest(WebView view, String url, Map<String, String> reqHeaders) {
        // https://hc.apache.org/
        // https://hc.apache.org/httpcomponents-client-ga/tutorial/html/statemgmt.html#d5e576
        // https://hc.apache.org/httpcomponents-client-4.3.x/android-port.html
        // https://hc.apache.org/httpcomponents-client-4.5.x/android-port.html
        // https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient
        // https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient-android
        // https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5
        // https://github.com/apache/httpcomponents-client
        // https://stackoverflow.com/a/29811280
        // https://stackoverflow.com/questions/32153318

        try {
            HttpGet request = new HttpGet(url);

            if ((reqHeaders != null) && (reqHeaders.size() > 0)) {
                for (Map.Entry<String,String> header : reqHeaders.entrySet()) {
                    request.setHeader(
                        header.getKey(),
                        header.getValue()
                    );
                }
            }

            HttpResponse response           = this.httpClient.execute(request, this.clientContext);
            Header contentType              = response.getEntity().getContentType();
            Header encoding                 = response.getEntity().getContentEncoding();
            InputStream responseInputStream = response.getEntity().getContent();

            String contentTypeValue = null;
            String encodingValue    = null;
            if (contentType != null) {
                contentTypeValue = contentType.getValue().toLowerCase();
            }
            if (encoding != null) {
                encodingValue = encoding.getValue();
            }

            if ((contentTypeValue != null) && this.browserActivity.isVideo(contentTypeValue)) {
                this.browserActivity.addSavedVideo(url, contentTypeValue);
            }

            return new WebResourceResponse(contentTypeValue, encodingValue, responseInputStream);
        }
        catch (ClientProtocolException e) {
            // try again
            return null;
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
            HttpHeaders.USER_AGENT,
            view.getSettings().getUserAgentString()
        );
        reqHeaders.put(
            HttpHeaders.REFERER,
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
