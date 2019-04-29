package com.github.warren_bank.webcast.exoplayer2;

final class VideoSource {

  public final String uri;
  public final String mimeType;
  public final String referer;

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
    this.uri      = uri;
    this.mimeType = mimeType.toLowerCase();
    this.referer  = referer;
  }

  // Public methods.

  @Override
  public String toString() {
    return uri;
  }

}
