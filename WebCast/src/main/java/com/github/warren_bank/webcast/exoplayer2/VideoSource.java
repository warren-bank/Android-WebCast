package com.github.warren_bank.webcast.exoplayer2;

final class VideoSource {

  public final String uri;
  public final String mimeType;

  // static factory

  public static VideoSource createVideoSource(
    String uri,
    String mimeType
  ) {
    VideoSource videoSource = new VideoSource(uri, mimeType);
    return videoSource;
  }

  private VideoSource(
    String uri,
    String mimeType
  ) {
    this.uri      = uri;
    this.mimeType = mimeType.toLowerCase();
  }

  // Public methods.

  @Override
  public String toString() {
    return uri;
  }

}
