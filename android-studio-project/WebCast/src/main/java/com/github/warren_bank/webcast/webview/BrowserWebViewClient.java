package com.github.warren_bank.webcast.webview;

import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BrowserWebViewClient extends WebViewClient {

    private BrowserActivity browserActivity;
    private Pattern video_regex;

    private void process_URL(String uri, WebView view) {
        Matcher matcher = video_regex.matcher(uri.toLowerCase());
        String file_ext;
        String mimeType;

        if (matcher.find()) {
            file_ext = matcher.group(1);

            switch (file_ext) {
                case "mp4":
                case "mp4v":
                case "m4v":
                    mimeType = "video/mp4";
                    break;
                case "mpv":
                    mimeType = "video/MPV";
                    break;
                case "m1v":
                case "mpg":
                case "mpg2":
                case "mpeg":
                    mimeType = "video/mpeg";
                    break;
                case "xvid":
                    mimeType = "video/x-xvid";
                    break;
                case "webm":
                    mimeType = "video/webm";
                    break;
                case "3gp":
                    mimeType = "video/3gpp";
                    break;
                case "avi":
                    mimeType = "video/x-msvideo";
                    break;
                case "mov":
                    mimeType = "video/quicktime";
                    break;
                case "mkv":
                    mimeType = "video/x-mkv";
                    break;
                case "ogg":
                case "ogv":
                case "ogm":
                    mimeType = "video/ogg";
                    break;
                case "m3u8":
                    mimeType = "application/x-mpegURL";
                    break;
                case "mpd":
                    mimeType = "application/dash+xml";
                    break;
                case "ism":
                case "ism/manifest":
                case "ismv":
                case "ismc":
                    mimeType = "application/vnd.ms-sstr+xml";
                    break;
                default:
                    return;
            }

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
        this.video_regex     = Pattern.compile("\\.(mp4|mp4v|mpv|m1v|m4v|mpg|mpg2|mpeg|xvid|webm|3gp|avi|mov|mkv|ogg|ogv|ogm|m3u8|mpd|ism(?:[vc]|/manifest)?)(?:[\\?#]|$)");
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
