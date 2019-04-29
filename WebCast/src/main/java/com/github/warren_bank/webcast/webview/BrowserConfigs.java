package com.github.warren_bank.webcast.webview;

import java.util.Map;
import java.util.Hashtable;

public class BrowserConfigs {

    public static Map<String, String> getDefaultBookmarks() {
        Hashtable<String, String> bookmarks = new Hashtable<String, String>();

        bookmarks.put(
            "-- blank --",
            "about:blank"
        );
        bookmarks.put(
            "Google",
            "https://www.google.com/"
        );
        bookmarks.put(
            "DuckDuckGo",
            "https://duckduckgo.com/"
        );
        bookmarks.put(
            "ABC News",
            "https://abclive2-lh.akamaihd.net/i/abc_live11@423404/master.m3u8"
        );
        bookmarks.put(
            "CBS News",
            "https://www.cbsnews.com/common/video/dai_prod.m3u8"
        );
        return bookmarks;
    }

}
