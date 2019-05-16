package com.github.warren_bank.webcast.exoplayer2;

import com.github.warren_bank.webcast.R;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.TimelineChangeReason;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;

import java.util.ArrayList;

/** Manages players and an internal media queue */
public final class PlayerManager
    implements EventListener, CastPlayer.SessionAvailabilityListener {

  /**
   * Listener for changes in the media queue playback position.
   */
  public interface QueuePositionListener {

    /**
     * Called when the currently played item of the media queue changes.
     */
    void onQueuePositionChanged(int previousIndex, int newIndex);

  }

  private QueuePositionListener queuePositionListener;
  private PlayerView localPlayerView;
  private PlayerControlView castControlView;
  private FullScreenManager fullScreenManager;
  private ArrayList<VideoSource> mediaQueue;
  private ConcatenatingMediaSource concatenatingMediaSource;
  private SimpleExoPlayer exoPlayer;
  private CastPlayer castPlayer;
  private DefaultHttpDataSourceFactory dataSourceFactory;

  private int currentItemIndex;
  private boolean castMediaQueueCreationPending;
  private Player currentPlayer;

  public static enum PlaybackMode { NORMAL, CAST_ONLY, RELEASED }

  private PlaybackMode playbackMode;

  /**
   * @param queuePositionListener A {@link QueuePositionListener} for queue position changes.
   * @param localPlayerView The {@link PlayerView} for local playback.
   * @param castControlView The {@link PlayerControlView} to control remote playback.
   * @param context A {@link Context}.
   * @param castContext The {@link CastContext}.
   */
  public static PlayerManager createPlayerManager(
      QueuePositionListener queuePositionListener,
      PlayerView localPlayerView,
      PlayerControlView castControlView,
      Context context,
      CastContext castContext,
      FullScreenManager fullScreenManager
    ) {
    PlayerManager playerManager = new PlayerManager(
        queuePositionListener, localPlayerView, castControlView, context, castContext, fullScreenManager
    );
    playerManager.init();
    return playerManager;
  }

  private PlayerManager(
      QueuePositionListener queuePositionListener,
      PlayerView localPlayerView,
      PlayerControlView castControlView,
      Context context,
      CastContext castContext,
      FullScreenManager fullScreenManager
    ) {
    this.queuePositionListener = queuePositionListener;
    this.localPlayerView = localPlayerView;
    this.castControlView = castControlView;
    this.fullScreenManager = fullScreenManager;
    this.mediaQueue = new ArrayList<>();
    this.concatenatingMediaSource = new ConcatenatingMediaSource();

    DefaultTrackSelector trackSelector = new DefaultTrackSelector();
    RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
    this.exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector);
    this.exoPlayer.addListener(this);
    this.localPlayerView.setPlayer(this.exoPlayer);

    ExoPlayerEventLogger exoLogger = new ExoPlayerEventLogger(trackSelector);
    this.exoPlayer.addListener(exoLogger);

    this.castPlayer = new CastPlayer(castContext);
    this.castPlayer.addListener(this);
    this.castPlayer.setSessionAvailabilityListener(this);
    this.castControlView.setPlayer(this.castPlayer);

    String userAgent = context.getResources().getString(R.string.user_agent);
    this.dataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);

    this.currentItemIndex = C.INDEX_UNSET;
    this.playbackMode = PlaybackMode.NORMAL;
  }

  // Queue manipulation methods.

  /**
   * Plays a specified queue item in the current player.
   *
   * @param itemIndex The index of the item to play.
   */
  public void selectQueueItem(int itemIndex) {
    setCurrentItem(itemIndex, C.TIME_UNSET, true);
  }

  /**
   * Returns the index of the currently played item.
   */
  public int getCurrentItemIndex() {
    return currentItemIndex;
  }

  /**
   * Appends {@code sample} to the media queue.
   *
   * @param sample The {@link VideoSource} to append.
   */
  public void addItem(VideoSource sample) {
    mediaQueue.add(sample);
    concatenatingMediaSource.addMediaSource(buildMediaSource(sample));
    if (currentPlayer == castPlayer) {
      castPlayer.addItems(buildMediaQueueItem(sample));
    }
  }

  /**
   * Returns the size of the media queue.
   */
  public int getMediaQueueSize() {
    return mediaQueue.size();
  }

  /**
   * Returns the item at the given index in the media queue.
   *
   * @param position The index of the item.
   * @return The item at the given index in the media queue.
   */
  public VideoSource getItem(int position) {
    return mediaQueue.get(position);
  }

  /**
   * Removes the item at the given index from the media queue.
   *
   * @param itemIndex The index of the item to remove.
   * @return Whether the removal was successful.
   */
  public boolean removeItem(int itemIndex) {
    concatenatingMediaSource.removeMediaSource(itemIndex);
    if (currentPlayer == castPlayer) {
      if (castPlayer.getPlaybackState() != Player.STATE_IDLE) {
        Timeline castTimeline = castPlayer.getCurrentTimeline();
        if (castTimeline.getPeriodCount() <= itemIndex) {
          return false;
        }
        castPlayer.removeItem((int) castTimeline.getPeriod(itemIndex, new Period()).id);
      }
    }
    mediaQueue.remove(itemIndex);
    if ((itemIndex == currentItemIndex) && (itemIndex == mediaQueue.size())) {
      maybeSetCurrentItemAndNotify(C.INDEX_UNSET);
    } else if (itemIndex < currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex - 1);
    }
    return true;
  }

  /**
   * Moves an item within the queue.
   *
   * @param fromIndex The index of the item to move.
   * @param toIndex The target index of the item in the queue.
   * @return Whether the item move was successful.
   */
  public boolean moveItem(int fromIndex, int toIndex) {
    // Player update.
    concatenatingMediaSource.moveMediaSource(fromIndex, toIndex);
    if ((currentPlayer == castPlayer) && (castPlayer.getPlaybackState() != Player.STATE_IDLE)) {
      Timeline castTimeline = castPlayer.getCurrentTimeline();
      int periodCount = castTimeline.getPeriodCount();
      if (periodCount <= fromIndex || periodCount <= toIndex) {
        return false;
      }
      int elementId = (int) castTimeline.getPeriod(fromIndex, new Period()).id;
      castPlayer.moveItem(elementId, toIndex);
    }

    mediaQueue.add(toIndex, mediaQueue.remove(fromIndex));

    // Index update.
    if (fromIndex == currentItemIndex) {
      maybeSetCurrentItemAndNotify(toIndex);
    } else if (fromIndex < currentItemIndex && toIndex >= currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex - 1);
    } else if (fromIndex > currentItemIndex && toIndex <= currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex + 1);
    }

    return true;
  }

  // Miscellaneous methods.

  /**
   * Is cast player currently active?
   */
  public boolean isCasting() {
    return (currentPlayer == castPlayer);
  }

  public PlaybackMode getPlaybackMode() {
    return playbackMode;
  }

  public void setPlaybackMode(PlaybackMode playbackMode) {
    switch (playbackMode) {
      case NORMAL:
        break;
      case CAST_ONLY:
        if (isCasting()) {
          release_exoPlayer();
        }
        else {
          release();
          playbackMode = PlaybackMode.RELEASED;
        }
        break;
      case RELEASED:
        release();
        break;
      default:
        return;
    }

    this.playbackMode = playbackMode;
  }

  /**
   * Dispatches a given {@link KeyEvent} to the corresponding view of the current player.
   *
   * @param event The {@link KeyEvent}.
   * @return Whether the event was handled by the target view.
   */
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (currentPlayer == exoPlayer) {
      return localPlayerView.dispatchKeyEvent(event);
    } else /* currentPlayer == castPlayer */ {
      return castControlView.dispatchKeyEvent(event);
    }
  }

  /**
   * Releases the manager and the players that it holds.
   */
  private void release() {
    if (playbackMode == PlaybackMode.RELEASED) return;

    try {
      release_exoPlayer();
      release_castPlayer();

      mediaQueue.clear();
      concatenatingMediaSource.clear();

      queuePositionListener = null;
      mediaQueue = null;
      concatenatingMediaSource = null;
      dataSourceFactory = null;
      currentItemIndex = C.INDEX_UNSET;
      currentPlayer = null;
    }
    catch (Exception e){}

    playbackMode = PlaybackMode.RELEASED;
  }

  private void release_exoPlayer() {
    if (exoPlayer == null) return;

    try {
      localPlayerView.setPlayer(null);
      fullScreenManager.release();
      exoPlayer.removeListener(this);
      exoPlayer.release();

      localPlayerView = null;
      fullScreenManager = null;
      exoPlayer = null;
    }
    catch (Exception e){}
  }

  private void release_castPlayer() {
    if (castPlayer == null) return;

    try {
      castControlView.setPlayer(null);
      castPlayer.removeListener(this);
      castPlayer.setSessionAvailabilityListener(null);
      castPlayer.release();

      castControlView = null;
      castPlayer = null;
      castMediaQueueCreationPending = false;
    }
    catch (Exception e){}
  }

  // Player.EventListener implementation.

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    updateCurrentItemIndex();

    if (playbackMode == PlaybackMode.CAST_ONLY) {
      switch(playbackState) {
        case Player.STATE_ENDED:
        case Player.STATE_IDLE:
          setPlaybackMode(PlaybackMode.RELEASED);
          break;
        default:
          break;
      }
    }
  }

  @Override
  public void onPositionDiscontinuity(@DiscontinuityReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onTimelineChanged(
      Timeline timeline, @Nullable Object manifest, @TimelineChangeReason int reason
  ){
    updateCurrentItemIndex();
    if (timeline.isEmpty()) {
      castMediaQueueCreationPending = true;
    }
  }

  @Override
  public void onPlayerError(ExoPlaybackException error) {
    if (playbackMode == PlaybackMode.CAST_ONLY) {
      setPlaybackMode(PlaybackMode.RELEASED);
    }
  }

  // CastPlayer.SessionAvailabilityListener implementation.

  @Override
  public void onCastSessionAvailable() {
    if (castPlayer == null) return;

    setCurrentPlayer(castPlayer);
  }

  @Override
  public void onCastSessionUnavailable() {
    if (castPlayer == null) return;

    if (playbackMode == PlaybackMode.CAST_ONLY) {
      setPlaybackMode(PlaybackMode.RELEASED);
    }
    else {
      setCurrentPlayer(exoPlayer);
    }
  }

  // Internal methods.

  private void init() {
    boolean isCasting = castPlayer.isCastSessionAvailable();

    setCurrentPlayer(isCasting ? castPlayer : exoPlayer);

    if (isCasting)
      fullScreenManager.disable();
    else
      fullScreenManager.enable();
  }

  private void updateCurrentItemIndex() {
    if (currentPlayer == null) return;

    int playbackState = currentPlayer.getPlaybackState();
    maybeSetCurrentItemAndNotify(
        playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
        ? currentPlayer.getCurrentWindowIndex() : C.INDEX_UNSET);
  }

  private void setCurrentPlayer(Player currentPlayer) {
    if (this.currentPlayer == currentPlayer) {
      return;
    }

    // View management.
    if (currentPlayer == exoPlayer) {
      fullScreenManager.enable();
      localPlayerView.setVisibility(View.VISIBLE);
      castControlView.hide();
    } else /* currentPlayer == castPlayer */ {
      fullScreenManager.disable();
      localPlayerView.setVisibility(View.GONE);
      castControlView.show();
    }

    // Player state management.
    long playbackPositionMs = C.TIME_UNSET;
    int windowIndex = C.INDEX_UNSET;
    boolean playWhenReady = false;
    if (this.currentPlayer != null) {
      int playbackState = this.currentPlayer.getPlaybackState();
      if (playbackState != Player.STATE_ENDED) {
        playbackPositionMs = this.currentPlayer.getCurrentPosition();
        playWhenReady = this.currentPlayer.getPlayWhenReady();
        windowIndex = this.currentPlayer.getCurrentWindowIndex();
        if (windowIndex != currentItemIndex) {
          playbackPositionMs = C.TIME_UNSET;
          windowIndex = currentItemIndex;
        }
      }
      this.currentPlayer.stop(true);
    } else {
      // This is the initial setup. No need to save any state.
    }

    this.currentPlayer = currentPlayer;

    // Media queue management.
    castMediaQueueCreationPending = currentPlayer == castPlayer;
    if (currentPlayer == exoPlayer) {
      exoPlayer.prepare(concatenatingMediaSource);
    }

    // Playback transition.
    if (windowIndex != C.INDEX_UNSET) {
      setCurrentItem(windowIndex, playbackPositionMs, playWhenReady);
    }
  }

  /**
   * Starts playback of the item at the given position.
   *
   * @param itemIndex The index of the item to play.
   * @param positionMs The position at which playback should start.
   * @param playWhenReady Whether the player should proceed when ready to do so.
   */
  private void setCurrentItem(int itemIndex, long positionMs, boolean playWhenReady) {
    maybeSetCurrentItemAndNotify(itemIndex);
    if (castMediaQueueCreationPending) {
      MediaQueueItem[] items = new MediaQueueItem[mediaQueue.size()];
      for (int i = 0; i < items.length; i++) {
        items[i] = buildMediaQueueItem(mediaQueue.get(i));
      }
      castMediaQueueCreationPending = false;
      castPlayer.loadItems(items, itemIndex, positionMs, Player.REPEAT_MODE_OFF);
    } else {
      currentPlayer.seekTo(itemIndex, positionMs);
      currentPlayer.setPlayWhenReady(playWhenReady);
    }
  }

  private void maybeSetCurrentItemAndNotify(int currentItemIndex) {
    if (this.currentItemIndex != currentItemIndex) {
      int oldIndex = this.currentItemIndex;
      this.currentItemIndex = currentItemIndex;
      queuePositionListener.onQueuePositionChanged(oldIndex, currentItemIndex);
    }
  }

  private void setHttpRequestHeader(String name, String value) {
    dataSourceFactory.getDefaultRequestProperties().set(name, value);
  }

  private MediaSource buildMediaSource(VideoSource sample) {
    Uri uri = Uri.parse(sample.uri);
    if (sample.referer != null) {
      Uri referer   = Uri.parse(sample.referer);
      String origin = referer.getScheme() + "://" + referer.getAuthority();

      setHttpRequestHeader("origin",  origin);
      setHttpRequestHeader("referer", sample.referer);
    }
    switch (sample.mimeType) {
      case MimeTypes.APPLICATION_M3U8:
        return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      case MimeTypes.APPLICATION_MPD:
        return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      case MimeTypes.APPLICATION_SS:
        return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      default:
        return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }
  }

  private MediaQueueItem buildMediaQueueItem(VideoSource sample) {
    MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
  //movieMetadata.putString(MediaMetadata.KEY_TITLE, sample.uri);

    MediaInfo mediaInfo = new MediaInfo.Builder(sample.uri)
        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setContentType(sample.mimeType)
        .setMetadata(movieMetadata)
        .build();

    return new MediaQueueItem.Builder(mediaInfo).build();
  }

}
