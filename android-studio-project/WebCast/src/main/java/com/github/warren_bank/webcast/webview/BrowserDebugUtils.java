package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.BuildConfig;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.webkit.WebView;
import android.widget.Toast;

public class BrowserDebugUtils {

    public static void configWebView(Context context) {
        if (Build.VERSION.SDK_INT >= 19) {  // Build.VERSION_CODES.KITKAT
            if (
                (BuildConfig.DEBUG)
             || (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            ) {
                WebView.setWebContentsDebuggingEnabled(true);

                Toast.makeText(context, "WebView remote debugging enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
