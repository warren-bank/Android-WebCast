package com.github.warren_bank.webcast.webview;

public class BrowserConfigs {

    public static String getDefaultBookmarks() {
        return "["
          /*
             + "  {"
             + "    \"title\":    \"---\","
             + "    \"uri\":      \"---\","
             + "    \"mimeType\": \"---\","
             + "    \"referer\":  \"---\""
             + "  },"
          */
             + "  {"
             + "    \"title\":    \"-- blank --\","
             + "    \"uri\":      \"about:blank\","
             + "    \"mimeType\": null,"
             + "    \"referer\":  null"
             + "  },"
             + "  {"
             + "    \"title\":    \"Google\","
             + "    \"uri\":      \"https://www.google.com/\","
             + "    \"mimeType\": null,"
             + "    \"referer\":  null"
             + "  },"
             + "  {"
             + "    \"title\":    \"DuckDuckGo\","
             + "    \"uri\":      \"https://duckduckgo.com/\","
             + "    \"mimeType\": null,"
             + "    \"referer\":  null"
             + "  },"
             + "  {"
             + "    \"title\":    \"ABC News\","
             + "    \"uri\":      \"https://abclive2-lh.akamaihd.net/i/abc_live11@423404/master.m3u8\","
             + "    \"mimeType\": \"application/x-mpegURL\","
             + "    \"referer\":  \"https://abcnews.go.com/Live\""
             + "  },"
             + "  {"
             + "    \"title\":    \"CBS News\","
             + "    \"uri\":      \"https://www.cbsnews.com/common/video/dai_prod.m3u8\","
             + "    \"mimeType\": \"application/x-mpegURL\","
             + "    \"referer\":  \"https://www.cbsnews.com/live/\""
             + "  }"
             + "]";
    }

}
