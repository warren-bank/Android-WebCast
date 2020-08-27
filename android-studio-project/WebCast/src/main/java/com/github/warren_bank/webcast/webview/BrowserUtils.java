package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.ViewGroup;

public class BrowserUtils {

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void hideKeyboard(Activity activity) {
        Context context = (Context) activity;
        View view       = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        hideKeyboard(context, view);
    }

    public static void resizeDrawerWidthByPercentOfScreen(Context context, View view, float percent) {
        int width_px;
        ViewGroup.LayoutParams params;

        width_px     = context.getResources().getDisplayMetrics().widthPixels;
        width_px     = (int)(width_px * (percent/100));

        params       = (ViewGroup.LayoutParams) view.getLayoutParams();
        params.width = width_px;

        view.setLayoutParams(params);
    }

    public static String getVideoPlayerPreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String pref_key         = context.getString(R.string.pref_videoplayer_key);
        String pref_default     = context.getString(R.string.pref_videoplayer_default);

        return prefs.getString(pref_key, pref_default);
    }

    public static int getIndexOfArray(Object needle, Object[] haystack) {
        for (int i=0; i < haystack.length; i++) {
            if (
                ((haystack[i] != null) && haystack[i].equals(needle))
             || ((haystack[i] == null) && (needle == null))
            ) {
                return i;
            }
        }
        return -1;
    }

    public static int getVideoPlayerPreferenceIndex(Context context) {
        String   pref_value  = BrowserUtils.getVideoPlayerPreference(context);
        String[] pref_values = context.getResources().getStringArray(R.array.pref_videoplayer_array_values);

        return BrowserUtils.getIndexOfArray(pref_value, pref_values);
    }

    public static String base64_encode(String input) {
        try {
            byte[] bytes  = input.getBytes("UTF-8");
            int flags     = Base64.NO_WRAP | Base64.URL_SAFE;
            String output = Base64.encodeToString(bytes, flags);
            return output;
        }
        catch (Exception e) {
            return null;
        }
    }

}
