package com.github.warren_bank.webcast.webview;

import android.app.Activity;
import android.content.Context;
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

}
