package com.github.warren_bank.webcast.exoplayer2;

import com.google.android.exoplayer2.MediaItem;

import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

final class VideoSource {

  public final String uri;
  public final String mimeType;
  public final String referer;
  public final HashMap<String, String> reqHeadersMap;

  // static factory

  public static VideoSource createVideoSource(
    String uri,
    String mimeType,
    String referer
  ) {
    VideoSource videoSource = new VideoSource(uri, mimeType, referer);
    return videoSource;
  }

  private VideoSource(
    String uri,
    String mimeType,
    String referer
  ) {
    this.uri           = uri;
    this.mimeType      = mimeType;
    this.referer       = referer;
    this.reqHeadersMap = new HashMap<String, String>();

    if (!TextUtils.isEmpty(referer)) {
      reqHeadersMap.put("referer", referer);

      Uri refererUri = Uri.parse(referer);
      String origin  = refererUri.getScheme() + "://" + refererUri.getAuthority();
      reqHeadersMap.put("origin", origin);
    }
  }

  // Public methods.

  @Override
  public String toString() {
    return uri;
  }

  public MediaItem getMediaItem() {
    MediaItem.Builder builder = new MediaItem.Builder();
    builder.setUri(uri);

    // ignore undefined mime-types
    if (!TextUtils.isEmpty(mimeType)) {
      builder.setMimeType(mimeType);
    }

    return builder.build();
  }

}
