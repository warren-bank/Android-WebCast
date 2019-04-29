package com.github.warren_bank.webcast.exoplayer2;

import com.github.warren_bank.webcast.R;

import android.support.v4.content.ContextCompat;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.PlayerView;

final class FullScreenManager {

  private boolean isEnabled;
  private boolean isFullScreen;
  private VideoActivity videoActivity;
  private PlayerView localPlayerView;
  private Dialog mFullScreenDialog;
  private ImageView mFullScreenIcon;
  private FrameLayout mFullScreenButton;

  // static factory

  public static FullScreenManager createFullScreenManager(
    VideoActivity videoActivity,
    PlayerView localPlayerView
  ) {
    FullScreenManager fullScreenManager = new FullScreenManager(videoActivity, localPlayerView);
    return fullScreenManager;
  }

  private FullScreenManager(
    VideoActivity videoActivity,
    PlayerView localPlayerView
  ) {
    this.videoActivity   = videoActivity;
    this.localPlayerView = localPlayerView;
    this.isEnabled       = true;
    this.isFullScreen    = false;

    initFullscreenDialog();
    initFullscreenButton();
  }

  // Public methods.

  public void enable() {
    isEnabled = true;
  }

  public void disable() {
    if (isFullScreen)
      closeFullscreenDialog();
    isEnabled = false;
  }

  public void release() {
    disable();

    if (mFullScreenDialog != null)
      mFullScreenDialog.dismiss();

    videoActivity     = null;
    localPlayerView   = null;
    mFullScreenDialog = null;
    mFullScreenIcon   = null;
    mFullScreenButton = null;
  }

  // Internal methods.

  private void initFullscreenDialog() {
    mFullScreenDialog = new Dialog(videoActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
        public void onBackPressed() {
          if (isFullScreen)
            closeFullscreenDialog();
          super.onBackPressed();
        }
    };
  }

  private void openFullscreenDialog() {
    ((ViewGroup) localPlayerView.getParent()).removeView(localPlayerView);
    mFullScreenDialog.addContentView(localPlayerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(videoActivity, R.drawable.ic_fullscreen_close));
    isFullScreen = true;
    mFullScreenDialog.show();
  }

  private void closeFullscreenDialog() {
    ((ViewGroup) localPlayerView.getParent()).removeView(localPlayerView);
    ((FrameLayout) videoActivity.findViewById(R.id.main_media_frame)).addView(localPlayerView);
    isFullScreen = false;
    mFullScreenDialog.dismiss();
    mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(videoActivity, R.drawable.ic_fullscreen_open));
  }

  private void initFullscreenButton() {
    PlaybackControlView controlView = localPlayerView.findViewById(R.id.exo_controller);
    mFullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);
    mFullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
    mFullScreenButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (!isEnabled)
            return;
          if (!isFullScreen)
            openFullscreenDialog();
          else
            closeFullscreenDialog();
        }
    });
  }

}
