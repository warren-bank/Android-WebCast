package com.github.warren_bank.webcast.exoplayer2;

import com.github.warren_bank.webcast.R;

import androidx.core.content.ContextCompat;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.media3.ui.PlayerView;

final class FullScreenManager implements PlayerView.FullscreenButtonClickListener {

  private boolean isEnabled;
  private boolean isFullScreen;
  private VideoActivity videoActivity;
  private PlayerView localPlayerView;
  private Dialog mFullScreenDialog;

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
    ((ViewGroup) localPlayerView.getParent()).removeView(localPlayerView);                     // FrameLayout
    mFullScreenDialog.addContentView(localPlayerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    isFullScreen = true;
    mFullScreenDialog.show();
  }

  private void closeFullscreenDialog() {
    ((ViewGroup) localPlayerView.getParent()).removeView(localPlayerView);                     // Dialog
    ((ViewGroup) videoActivity.findViewById(R.id.main_media_frame)).addView(localPlayerView);  // FrameLayout
    isFullScreen = false;
    mFullScreenDialog.dismiss();
  }

  private void initFullscreenButton() {
    localPlayerView.setFullscreenButtonClickListener(this);
  }

  // PlayerView.FullscreenButtonClickListener implementation.

  @Override
  public void onFullscreenButtonClick​(boolean isFullScreen) {
    if (isFullScreen)
      openFullscreenDialog();
    else
      closeFullscreenDialog();
  }

}
