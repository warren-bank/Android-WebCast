package com.google.android.exoplayer2.fullscreendemo;

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
  private MainActivity mainActivity;
  private PlayerView localPlayerView;
  private Dialog mFullScreenDialog;
  private ImageView mFullScreenIcon;
  private FrameLayout mFullScreenButton;

  // static factory

  public static FullScreenManager createFullScreenManager(
    MainActivity mainActivity,
    PlayerView localPlayerView
  ) {
    FullScreenManager fullScreenManager = new FullScreenManager(mainActivity, localPlayerView);
    return fullScreenManager;
  }

  private FullScreenManager(
    MainActivity mainActivity,
    PlayerView localPlayerView
  ) {
    this.mainActivity    = mainActivity;
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

    mainActivity      = null;
    localPlayerView   = null;
    mFullScreenDialog = null;
    mFullScreenIcon   = null;
    mFullScreenButton = null;
  }

  // Internal methods.

  private void initFullscreenDialog() {
    mFullScreenDialog = new Dialog(mainActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
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
    mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_fullscreen_skrink));
    isFullScreen = true;
    mFullScreenDialog.show();
  }

  private void closeFullscreenDialog() {
    ((ViewGroup) localPlayerView.getParent()).removeView(localPlayerView);
    ((FrameLayout) mainActivity.findViewById(R.id.main_media_frame)).addView(localPlayerView);
    isFullScreen = false;
    mFullScreenDialog.dismiss();
    mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_fullscreen_expand));
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
