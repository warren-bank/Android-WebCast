package com.github.warren_bank.webcast.webview;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.view.View;

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

    public static void hideKeyboard(Fragment fragment) {
        Context context = fragment.getContext();
        View view       = fragment.getView().getRootView();

        hideKeyboard(context, view);
    }

}
