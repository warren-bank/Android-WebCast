package com.github.warren_bank.webcast.exoplayer2;

import com.github.warren_bank.webcast.R;
import com.github.warren_bank.webcast.SharedUtils;
import com.github.warren_bank.webcast.WebCastApplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.dynamite.DynamiteModule;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class VideoActivity extends AppCompatActivity implements PlayerManager.QueuePositionListener {
  private static PlayerManager playerManager = null;

  private PlayerView localPlayerView;
  private PlayerControlView castControlView;
  private RecyclerView mediaQueueList;
  private MediaQueueListAdapter mediaQueueListAdapter;
  private CastContext castContext;

  // Activity lifecycle methods.

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Getting the cast context later than onStart can cause device discovery not to take place.
    try {
      castContext = CastContext.getSharedInstance(this);

      String receiverApplicationId = getResources().getString(R.string.cast_receiver_id);
      castContext.setReceiverApplicationId(receiverApplicationId);
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      while (cause != null) {
        if (cause instanceof DynamiteModule.LoadingException) {
          setContentView(R.layout.cast_context_error_message_layout);
          return;
        }
        cause = cause.getCause();
      }
      // Unknown error. We propagate it.
      throw e;
    }

    setContentView(R.layout.video_activity);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle("");
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onBackPressed();
      }
    });

    localPlayerView = (PlayerView) findViewById(R.id.local_player_view);
    localPlayerView.requestFocus();

    castControlView = (PlayerControlView) findViewById(R.id.cast_control_view);

    mediaQueueList = (RecyclerView) findViewById(R.id.sample_list);
    ItemTouchHelper helper = new ItemTouchHelper(new RecyclerViewCallback());
    helper.attachToRecyclerView(mediaQueueList);
    mediaQueueList.setLayoutManager(new LinearLayoutManager(this));
    mediaQueueList.setHasFixedSize(true);
    mediaQueueListAdapter = new MediaQueueListAdapter();

    // add divider between list items
    mediaQueueList.addItemDecoration(
        new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
    );
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.video, menu);
    CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
    return true;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (castContext == null) {
      // There is no Cast context to work with. Do nothing.
      return;
    }

    if ((playerManager != null) && (playerManager.getPlaybackMode() == PlayerManager.PlaybackMode.CAST_ONLY)) {
        playerManager.setPlaybackMode(PlayerManager.PlaybackMode.RELEASED_ALL_BUT_CAST_SESSION);
        playerManager = null;
    }

    if ((playerManager == null) || (playerManager.getPlaybackMode() == PlayerManager.PlaybackMode.RELEASED)) {
        FullScreenManager fullScreenManager =
            FullScreenManager.createFullScreenManager(
                /* videoActivity= */ this,
                localPlayerView
            );

        playerManager =
            PlayerManager.createPlayerManager(
                /* queuePositionListener= */ this,
                localPlayerView,
                castControlView,
                /* context= */ this,
                castContext,
                fullScreenManager
            );

        mediaQueueList.setAdapter(mediaQueueListAdapter);
        if (mediaQueueListAdapter.getItemCount() == 0) {
            addVideoSources();
        }
    }

    WebCastApplication.activityResumed(playerManager);

    WakeLockMgr.acquire(this);
    WifiLockMgr.acquire(this);
  }

  @Override
  public void onPause() {
    super.onPause();

    if (castContext == null) {
      // Nothing to release.
      return;
    }

    boolean isScreenOn = VideoUtils.isScreenOn(this);
    if (!isScreenOn) return;

    boolean isCasting = playerManager.isCasting();
    if (isCasting) {
        // if casting is interrupted, do NOT resume playback in ExoPlayer in the background
        playerManager.setPlaybackMode(PlayerManager.PlaybackMode.CAST_ONLY);

        WebCastApplication.activityPaused();
    }
    else {
        // cleanup all resources
        mediaQueueList.setAdapter(null);

        playerManager.setPlaybackMode(PlayerManager.PlaybackMode.RELEASED);
        playerManager = null;

        WebCastApplication.activityPaused(playerManager);
    }

    WakeLockMgr.release();
    WifiLockMgr.release();
  }

  // Activity input.

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // If the event was not handled then see if the player view can handle it.
    return super.dispatchKeyEvent(event) || playerManager.dispatchKeyEvent(event);
  }

  // PlayerManager.QueuePositionListener implementation.

  @Override
  public void onQueuePositionChanged(int previousIndex, int newIndex) {
    if (previousIndex != C.INDEX_UNSET) {
      mediaQueueListAdapter.notifyItemChanged(previousIndex);
    }
    if (newIndex != C.INDEX_UNSET) {
      mediaQueueListAdapter.notifyItemChanged(newIndex);
    }
  }

  // Internal methods.

  private void addVideoSource(String uri, String mimeType, String referer) {
    if (uri == null) return;

    if (mimeType == null)
      mimeType = SharedUtils.getVideoMimeType(uri);

    VideoSource videoSource = VideoSource.createVideoSource(uri, mimeType, referer);
    playerManager.addItem(videoSource);
    mediaQueueListAdapter.notifyItemInserted(playerManager.getMediaQueueSize() - 1);
  }

  private void addVideoSources() {
    Intent intent = getIntent();

    if (intent.hasExtra("video_sources")) {
      // explicit intent started by BrowserActivity

      String jsonSources = intent.getStringExtra("video_sources");

      Gson gson = new Gson();
      ArrayList<String> arrSources = gson.fromJson(jsonSources, new TypeToken<ArrayList<String>>() {
      }.getType());

      int strings_per_video = 3;
      int remainder = arrSources.size() % strings_per_video;
      int maxIndex  = (remainder == 0) ? arrSources.size() : (arrSources.size() - remainder);

      for (int i=0; i < maxIndex; i = i + strings_per_video) {
        addVideoSource(
          /* uri=      */ arrSources.get(i),
          /* mimeType= */ arrSources.get(i+1),
          /* referer=  */ arrSources.get(i+2)
        );
      }
    }
    else {
      // implicit intent (compatible with ExoAirPlayer and ExoAirPlayerSenderActivity)

      addVideoSource(
        /* uri=      */ intent.getDataString(),
        /* mimeType= */ null,
        /* referer=  */ intent.getStringExtra("referUrl")
      );
    }
  }

  // Internal classes.

  private class QueueItemViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

    public final TextView textView;

    public QueueItemViewHolder(TextView textView) {
      super(textView);
      this.textView = textView;
      textView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
      playerManager.selectQueueItem(getAdapterPosition());
    }

  }

  private class MediaQueueListAdapter extends RecyclerView.Adapter<QueueItemViewHolder> {

    @Override
    public QueueItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      TextView v = (TextView) LayoutInflater.from(parent.getContext())
          .inflate(android.R.layout.simple_list_item_1, parent, false);
      return new QueueItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(QueueItemViewHolder holder, int position) {
      TextView view = holder.textView;
      view.setText(playerManager.getItem(position).toString());
      // TODO: Solve coloring using the theme's ColorStateList.
      view.setTextColor(ColorUtils.setAlphaComponent(view.getCurrentTextColor(),
           position == playerManager.getCurrentItemIndex() ? 255 : 100));
    }

    @Override
    public int getItemCount() {
      return playerManager.getMediaQueueSize();
    }

  }

  private class RecyclerViewCallback extends ItemTouchHelper.SimpleCallback {

    private int draggingFromPosition;
    private int draggingToPosition;

    public RecyclerViewCallback() {
      super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.START | ItemTouchHelper.END);
      draggingFromPosition = C.INDEX_UNSET;
      draggingToPosition = C.INDEX_UNSET;
    }

    @Override
    public boolean onMove(RecyclerView list, RecyclerView.ViewHolder origin,
        RecyclerView.ViewHolder target) {
      int fromPosition = origin.getAdapterPosition();
      int toPosition = target.getAdapterPosition();
      if (draggingFromPosition == C.INDEX_UNSET) {
        // A drag has started, but changes to the media queue will be reflected in clearView().
        draggingFromPosition = fromPosition;
      }
      draggingToPosition = toPosition;
      mediaQueueListAdapter.notifyItemMoved(fromPosition, toPosition);
      return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
      int position = viewHolder.getAdapterPosition();
      if (playerManager.removeItem(position)) {
        mediaQueueListAdapter.notifyItemRemoved(position);
      }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
      super.clearView(recyclerView, viewHolder);
      if (draggingFromPosition != C.INDEX_UNSET) {
        // A drag has ended. We reflect the media queue change in the player.
        if (!playerManager.moveItem(draggingFromPosition, draggingToPosition)) {
          // The move failed. The entire sequence of onMove calls since the drag started needs to be
          // invalidated.
          mediaQueueListAdapter.notifyDataSetChanged();
        }
      }
      draggingFromPosition = C.INDEX_UNSET;
      draggingToPosition = C.INDEX_UNSET;
    }
  }
}
