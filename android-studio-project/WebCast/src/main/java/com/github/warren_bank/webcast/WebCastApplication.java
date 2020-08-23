package com.github.warren_bank.webcast;

import com.github.warren_bank.webcast.exoplayer2.PlayerManager;

import android.app.Application;
import android.os.Process;
import android.util.Log;

public class WebCastApplication extends Application {
    // ---------------------------------------------------------------------------------------------
    // counter to track visible activities

    private static int visibleActivityCounter;
    private static PlayerManager playerManager;

    public static void activityResumed() {
        visibleActivityCounter++;
    }

    public static void activityPaused() {
        visibleActivityCounter--;
    }

    public static void activityResumed(PlayerManager manager) {
        playerManager = manager;
        activityResumed();
    }

    public static void activityPaused(PlayerManager manager) {
        playerManager = manager;
        activityPaused();
    }

    // ---------------------------------------------------------------------------------------------
    // customize the handling of an uncaught exception (ie: crash)

    private Thread.UncaughtExceptionHandler androidDefaultUEH;
    private Thread.UncaughtExceptionHandler androidCustomUEH;

    @Override
    public void onCreate() {
        super.onCreate();

        visibleActivityCounter = 0;
        playerManager = null;

        androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();

        androidCustomUEH = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable cause) {
                if (playerManager != null) {
                    playerManager.setPlaybackMode(PlayerManager.PlaybackMode.RELEASED);
                    playerManager = null;
                }

                if (visibleActivityCounter <= 0) {
                    Log.e("WebCast", "Uncaught Exception: ", cause);

                    // fail silently
                    Process.killProcess(Process.myPid());
                }
                else {
                    // fallback to the default handler, which shows an error dialog
                    androidDefaultUEH.uncaughtException(thread, cause);
                }
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(androidCustomUEH);
    }
}
