package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.WebCastApplication;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        WebCastApplication.activityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        WebCastApplication.activityPaused();
    }
}
