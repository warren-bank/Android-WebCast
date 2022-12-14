package com.github.warren_bank.webcast.exoplayer2;

import com.github.warren_bank.webcast.R;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.cast.framework.CastContext;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Clock;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

/** Manages players and an internal media queue */
public final class PlayerManager implements Player.Listener, SessionAvailabilityListener {

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
  private ExoPlayer exoPlayer;
  private CastPlayer castPlayer;
  private DefaultHttpDataSource.Factory dataSourceFactory;

  private int currentItemIndex;
  private boolean castMediaQueueCreationPending;
  private Player currentPlayer;

  public static enum PlaybackMode { NORMAL, CAST_ONLY, RELEASED_ALL_BUT_CAST_SESSION, RELEASED }

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

    EventLogger exoLogger = new EventLogger(trackSelector);
    DefaultAnalyticsCollector analyticsCollector = new DefaultAnalyticsCollector(Clock.DEFAULT);
    analyticsCollector.addListener(exoLogger);

    this.exoPlayer = new ExoPlayer.Builder(context)
      .setTrackSelector(trackSelector)
      .setAnalyticsCollector(analyticsCollector)
      .build();

    this.exoPlayer.addListener(this);
    this.localPlayerView.setPlayer(this.exoPlayer);

    this.castPlayer = new CastPlayer(castContext);
    this.castPlayer.addListener(this);
    this.castPlayer.setSessionAvailabilityListener(this);
    this.castControlView.setPlayer(this.castPlayer);

    String userAgent = context.getResources().getString(R.string.user_agent);
    this.dataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(userAgent);

    this.currentItemIndex = C.INDEX_UNSET;
    this.playbackMode = PlaybackMode.NORMAL;
  }

  // ===========================================================================
  // Queue manipulation methods.
  // ===========================================================================

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
      int lastIndex = castPlayer.getCurrentTimeline().getWindowCount() - 1;
      List<MediaItem> mediaItems = new ArrayList<MediaItem>();
      mediaItems.add(sample.getMediaItem());

      castPlayer.addMediaItems(lastIndex, mediaItems);
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
    return ((position >= 0) && (getMediaQueueSize() > position))
      ? mediaQueue.get(position)
      : null;
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
        castPlayer.removeMediaItems(itemIndex, (itemIndex + 1));
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
      castPlayer.moveMediaItems(fromIndex, (fromIndex + 1), toIndex);
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

  // ===========================================================================
  // Miscellaneous methods.
  // ===========================================================================

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
          release(false);
          playbackMode = PlaybackMode.RELEASED;
        }
        break;
      case RELEASED_ALL_BUT_CAST_SESSION:
        release(
          isCasting()
        );
        playbackMode = PlaybackMode.RELEASED;
        break;
      case RELEASED:
        release(false);
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
    release(false);
  }

  private void release(boolean retain_CastSession) {
    if (playbackMode == PlaybackMode.RELEASED) return;

    try {
      release_exoPlayer();
      release_castPlayer(retain_CastSession);

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

  private void release_castPlayer(boolean retain_CastSession) {
    if (castPlayer == null) return;

    try {
      castControlView.setPlayer(null);
      castPlayer.removeListener(this);
      castPlayer.setSessionAvailabilityListener(null);

      if (!retain_CastSession) {
        castPlayer.release();
      }

      castControlView = null;
      castPlayer = null;
      castMediaQueueCreationPending = false;
    }
    catch (Exception e){}
  }

  // ===========================================================================
  // https://github.com/androidx/media/blob/1.0.0-beta03/libraries/common/src/main/java/androidx/media3/common/Player.java#L625-L1073
  // ===========================================================================
  // Player.Listener implementation.
  // ===========================================================================

  @Override
  public void onPlaybackStateChanged(@Player.State int playbackState) {
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
  public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onTimelineChanged(Timeline timeline, @Player.TimelineChangeReason int reason) {
    updateCurrentItemIndex();
    if (timeline.isEmpty()) {
      castMediaQueueCreationPending = true;
    }
  }

  @Override
  public void onPlayerError(PlaybackException error) {
    if (playbackMode == PlaybackMode.CAST_ONLY) {
      setPlaybackMode(PlaybackMode.RELEASED);
    }
  }

  // ===========================================================================
  // SessionAvailabilityListener implementation.
  // ===========================================================================

  @Override
  public void onCastSessionAvailable() {
    if (castPlayer == null) return;

    setCurrentPlayer(castPlayer);
  }

  @Override
  public void onCastSessionUnavailable() {
    if (castPlayer == null) return;

    if (playbackMode == PlaybackMode.CAST_ONLY) {
      setPlaybackMode(PlaybackMode.RELEASED_ALL_BUT_CAST_SESSION);
    }
    else {
      setCurrentPlayer(exoPlayer);
    }
  }

  // ===========================================================================
  // Internal methods.
  // ===========================================================================

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
      ((ViewGroup) localPlayerView.getParent()).setVisibility(View.VISIBLE);  // FrameLayout
      castControlView.hide();
    } else /* currentPlayer == castPlayer */ {
      fullScreenManager.disable();
      ((ViewGroup) localPlayerView.getParent()).setVisibility(View.GONE);     // FrameLayout
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
      List<MediaItem> mediaItems = new ArrayList<MediaItem>();
      for (int i = 0; i < mediaQueue.size(); i++) {
        VideoSource sample = mediaQueue.get(i);
        mediaItems.add(sample.getMediaItem());
      }
      castMediaQueueCreationPending = false;
      castPlayer.setMediaItems(mediaItems, itemIndex, positionMs);
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

      if (currentItemIndex != C.INDEX_UNSET) {
        setHttpRequestHeaders(currentItemIndex);
      }
    }
  }

  private void setHttpRequestHeaders(int currentItemIndex) {
    if (dataSourceFactory == null) return;

    VideoSource sample = getItem(currentItemIndex);
    if (sample == null) return;

    if (sample.reqHeadersMap != null) {
      dataSourceFactory.setDefaultRequestProperties(sample.reqHeadersMap);
    }
  }

  private MediaSource buildMediaSource(VideoSource sample) {
    MediaItem mediaItem = sample.getMediaItem();

    switch (sample.mimeType) {
      case MimeTypes.APPLICATION_M3U8:
        return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
      case MimeTypes.APPLICATION_MPD:
        return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
      case MimeTypes.APPLICATION_SS:
        return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
      default:
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
    }
  }

}
