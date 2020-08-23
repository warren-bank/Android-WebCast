package com.github.warren_bank.webcast.exoplayer2;

import android.content.Context;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;

import java.util.List;

public final class CastOptionsProvider implements OptionsProvider {

  @Override
  public CastOptions getCastOptions(Context context) {
    CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
      .setMediaIntentReceiverClassName(MediaIntentReceiver.class.getName())
      .build();

    return new CastOptions.Builder()
      .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
      .setCastMediaOptions(mediaOptions)
      .setStopReceiverApplicationWhenEndingSession(true)
      .build();
  }

  @Override
  public List<SessionProvider> getAdditionalSessionProviders(Context context) {
    return null;
  }

}
