package com.github.warren_bank.webcast.webview;

import com.github.warren_bank.webcast.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;

public class BrowserActivity extends AppCompatActivity {

    // ---------------------------------------------------------------------------------------------
    // Data Structures:
    // ---------------------------------------------------------------------------------------------

    // Stored Preferences --------------------------------------------------------------------------

    private static final String PREFS_FILENAME = "DRAWER_LIST_ITEMS";
    private static final String PREF_BOOKMARKS = "BOOKMARKS";

    // Nested Class Definitions --------------------------------------------------------------------

    private static final class DrawerListItem {
        public final String uri;
        public final String title;
        public final String mimeType;
        public final String referer;

        public DrawerListItem(String uri, String title, String mimeType, String referer) {
            this.uri      = uri;
            this.title    = title;
            this.mimeType = mimeType;
            this.referer  = referer;
        }

        @Override
        public String toString() {
            return ((title != null) && (title.length() > 0)) ? title : uri;
        }

        public boolean equal(DrawerListItem that) {
            return (this.uri.equals(that.uri));
        }

        public boolean equal(String that_uri) {
            return (this.uri.equals(that_uri));
        }

        public static ArrayList<DrawerListItem> fromJson(String jsonBookmarks) {
            ArrayList<DrawerListItem> arrayList;
            Gson gson = new Gson();
            arrayList = gson.fromJson(jsonBookmarks, new TypeToken<ArrayList<DrawerListItem>>(){}.getType());
            return arrayList;
        }

        // helpers

        public static boolean contains(ArrayList<DrawerListItem> items, DrawerListItem item) {
            for (int i=0; i < items.size(); i++) {
                DrawerListItem nextItem = items.get(i);
                if (nextItem.equal(item)) return true;
            }
            return false;
        }

        public static boolean contains(ArrayList<DrawerListItem> items, String uri) {
            for (int i=0; i < items.size(); i++) {
                DrawerListItem nextItem = items.get(i);
                if (nextItem.equal(uri)) return true;
            }
            return false;
        }
    }

    // Drawers -------------------------------------------------------------------------------------

    private DrawerLayout                 drawer_layout;

    private ListView                     drawer_left_bookmarks_listView;
    private ArrayList<DrawerListItem>    drawer_left_bookmarks_arrayList;
    private ArrayAdapter<DrawerListItem> drawer_left_bookmarks_arrayAdapter;

    private ListView                     drawer_right_videos_listView;
    private ArrayList<DrawerListItem>    drawer_right_videos_arrayList;
    private ArrayAdapter<DrawerListItem> drawer_right_videos_arrayAdapter;

    // Content: WebView ----------------------------------------------------------------------------

    private String default_page_url = "about:blank";
    private String current_page_url = default_page_url;
    private WebView webView;
    private BrowserWebViewClient webViewClient;
    private BrowserDownloadListener downloadListener;
    private ProgressBar progressBar;

    // Content: Search Form ------------------------------------------------------------------------

    private SearchView search;

    // Content: UI ---------------------------------------------------------------------------------

    private View parentView;
    private Snackbar snackbar;
    private AlertDialog alertDialog;

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Events:
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_activity);

        // Drawers ---------------------------------------------------------------------------------

        initDrawers();

        // Content: ActionBar ----------------------------------------------------------------------

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationIcon(null);

        // Content: WebView ------------------------------------------------------------------------

        if (getIntent().getExtras() != null) {
            current_page_url = getIntent().getStringExtra("url");
        }

        webView     = (WebView)findViewById(R.id.webView);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        initWebView();

        // Content: Search Form --------------------------------------------------------------------

        search = (SearchView)findViewById(R.id.search);

        search.setIconifiedByDefault(false);
        search.setSubmitButtonEnabled(true);

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String url = query.contains(":") ? query : ("http://" + query);

                updateCurrentPage(url, true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // noop
                return true;
            }
        });

        // Content: UI -----------------------------------------------------------------------------

        parentView = (View)findViewById(R.id.main_content);

        updateCurrentPage(current_page_url, true);
    }

    @Override
    public void onResume() {
        super.onResume();

        webView.loadUrl(current_page_url);
    }

    @Override
    public void onPause() {
        super.onPause();

        webView.loadUrl(default_page_url);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        webView.clearCache(true);
        webView.clearHistory();
    }

    // ---------------------------------------------------------------------------------------------
    // User Events:
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        if ((alertDialog instanceof AlertDialog) && alertDialog.isShowing()) {
            alertDialog.dismiss();
            return;
        }

        if ((snackbar instanceof Snackbar) && snackbar.isShown()) {
            snackbar.dismiss();
            return;
        }

        if (closeDrawerVideos()) {
            return;
        }

        if (closeDrawerBookmarks()) {
            return;
        }

        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
    }

    // ---------------------------------------------------------------------------------------------
    // Bookmarks:
    // ---------------------------------------------------------------------------------------------

    private ArrayList<DrawerListItem> getSavedBookmarks() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
        String jsonBookmarks = sharedPreferences.getString(PREF_BOOKMARKS, null);

        if (jsonBookmarks == null) {
            jsonBookmarks = BrowserConfigs.getDefaultBookmarks();

            // update SharedPreferences
            SharedPreferences.Editor prefs_editor = sharedPreferences.edit();
            prefs_editor.putString(PREF_BOOKMARKS, jsonBookmarks);
            prefs_editor.apply();
        }

        ArrayList<DrawerListItem> savedBookmarks = DrawerListItem.fromJson(jsonBookmarks);
        return savedBookmarks;
    }

    private void updateSavedBookmark(DrawerListItem item, boolean is_add) {
        boolean is_saved = DrawerListItem.contains(drawer_left_bookmarks_arrayList, item);
        String message = null;

        // sanity checks
        if (is_add && is_saved) {
            message = "Bookmarked";
        }
        if (!is_add && !is_saved) {
            message = "Bookmark Removed";
        }
        if ((message != null) && (message.length() > 0)) {
            snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_SHORT);
            snackbar.show();
            return;
        }

        if (is_add) {
            drawer_left_bookmarks_arrayList.add(item);
            message = "Bookmarked";
        }
        else {
            drawer_left_bookmarks_arrayList.remove(item);
            message = "Bookmark Removed";
        }

        // notify the ListView adapter
        drawer_left_bookmarks_arrayAdapter.notifyDataSetChanged();

        // update SharedPreferences
        SharedPreferences sharedPreferences   = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = sharedPreferences.edit();
        prefs_editor.putString(PREF_BOOKMARKS, new Gson().toJson(drawer_left_bookmarks_arrayList));
        prefs_editor.apply();

        // show message
        snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_SHORT);
        snackbar.show();

        // update 'bookmark' icon in top ActionBar
        invalidateOptionsMenu();
    }

    private void toggleSavedBookmark(DrawerListItem item) {
        boolean is_add = (DrawerListItem.contains(drawer_left_bookmarks_arrayList, item) == false);
        updateSavedBookmark(item, is_add);
    }

    private void addSavedBookmark(DrawerListItem item) {
        updateSavedBookmark(item, true);
    }

    private void removeSavedBookmark(DrawerListItem item) {
        updateSavedBookmark(item, false);
    }

    private void confirm_removeSavedBookmark(DrawerListItem item) {
        alertDialog = new AlertDialog.Builder(BrowserActivity.this)
            .setTitle("DELETE")
            .setMessage("Confirm that you want to delete this bookmark?")
            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    removeSavedBookmark(item);
                }
            })
            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            })
            .show();
    }

    // ---------------------------------------------------------------------------------------------
    // Videos:
    // ---------------------------------------------------------------------------------------------

    private void resetSavedVideos() {
        drawer_right_videos_arrayList.clear();

        // notify the ListView adapter
        drawer_right_videos_arrayAdapter.notifyDataSetChanged();
    }

    protected void addSavedVideo(String uri, String mimeType, String referer) {
        if (DrawerListItem.contains(drawer_right_videos_arrayList, uri)) return;

        DrawerListItem item = new DrawerListItem(
            uri,
            /* title= */ null,
            mimeType,
            referer
        );

        drawer_right_videos_arrayList.add(item);

        // notify the ListView adapter
        drawer_right_videos_arrayAdapter.notifyDataSetChanged();
    }

    private void removeSavedVideo(DrawerListItem item) {
        if (drawer_right_videos_arrayList.remove(item)) {
            // notify the ListView adapter
            drawer_right_videos_arrayAdapter.notifyDataSetChanged();
        }
    }

    private void confirm_removeSavedVideo(DrawerListItem item) {
        alertDialog = new AlertDialog.Builder(BrowserActivity.this)
            .setTitle("DELETE")
            .setMessage("Confirm that you want to delete this video?")
            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    removeSavedVideo(item);
                }
            })
            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            })
            .show();
    }

    protected boolean isVideo(String mimeType) {
        if (mimeType == null) return false;

        switch (mimeType) {
            case MimeTypes.APPLICATION_M3U8:
            case MimeTypes.APPLICATION_MPD:
            case MimeTypes.APPLICATION_SS:
                return true;
            default:
                return MimeTypes.isVideo(mimeType);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Drawers:
    // ---------------------------------------------------------------------------------------------

    private void initDrawerBookmarks() {
        drawer_left_bookmarks_listView.setAdapter(drawer_left_bookmarks_arrayAdapter);

        drawer_left_bookmarks_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DrawerListItem item = (DrawerListItem) parent.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this)
                    .setTitle("Bookmark URL")
                    .setMessage(item.uri)
                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                if (isVideo(item.mimeType)) {
                    builder.setPositiveButton("Watch", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            closeDrawerBookmarks();

                            openVideo(item);
                        }
                    });
                }
                else {
                    builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            closeDrawerBookmarks();

                            updateCurrentPage(item.uri, true);
                        }
                    });
                }

                alertDialog = builder.show();
            }
        });

        drawer_left_bookmarks_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                DrawerListItem item = (DrawerListItem) parent.getItemAtPosition(position);
                confirm_removeSavedBookmark(item);
                return true;
            }
        });
    }

    private void initDrawerVideos() {
        drawer_right_videos_listView.setAdapter(drawer_right_videos_arrayAdapter);

        drawer_right_videos_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DrawerListItem item = (DrawerListItem) parent.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this)
                    .setTitle("Video URL")
                    .setMessage(item.uri)
                    // button #1 of 3
                    .setPositiveButton("Watch", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            closeDrawerVideos();

                            openVideo(item);
                        }
                    })
                    // button #3 of 3
                    .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                if (DrawerListItem.contains(drawer_left_bookmarks_arrayList, item) == false) {
                    // button #2 of 3
                    builder.setNegativeButton("Bookmark", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            addSavedBookmark(item);
                        }
                    });
                }

                alertDialog = builder.show();
            }
        });

        drawer_right_videos_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                DrawerListItem item = (DrawerListItem) parent.getItemAtPosition(position);
                confirm_removeSavedVideo(item);
                return true;
            }
        });
    }

    private void initDrawers() {
        drawer_layout                      = (DrawerLayout)findViewById(R.id.drawer_layout);

        drawer_left_bookmarks_listView     = (ListView)findViewById(R.id.drawer_left_bookmarks);
        drawer_left_bookmarks_arrayList    = getSavedBookmarks();
        drawer_left_bookmarks_arrayAdapter = new ArrayAdapter<DrawerListItem>(BrowserActivity.this, R.layout.singleline_listitem, drawer_left_bookmarks_arrayList);

        drawer_right_videos_listView       = (ListView)findViewById(R.id.drawer_right_videos);
        drawer_right_videos_arrayList      = new ArrayList<DrawerListItem>();
        drawer_right_videos_arrayAdapter   = new ArrayAdapter<DrawerListItem>(BrowserActivity.this, R.layout.singleline_listitem, drawer_right_videos_arrayList);

        initDrawerBookmarks();
        initDrawerVideos();
    }

    private void toggleDrawer(View drawer, boolean animate) {
        if (drawer_layout.isDrawerOpen(drawer)) {
            drawer_layout.closeDrawer(drawer, animate);
        }
        else {
            drawer_layout.openDrawer( drawer, animate);
        }
    }

    private void toggleDrawer(View drawer) {
        toggleDrawer(drawer, true);
    }

    private void toggleDrawerBookmarks() {
        View drawer = (View)drawer_left_bookmarks_listView;
        toggleDrawer(drawer);
    }

    private void toggleDrawerVideos() {
        View drawer = (View)drawer_right_videos_listView;
        toggleDrawer(drawer);
    }

    private boolean closeDrawer(View drawer, boolean animate) {
        boolean was_open = drawer_layout.isDrawerOpen(drawer);

        if (was_open) {
            drawer_layout.closeDrawer(drawer, animate);
        }
        return was_open;
    }

    private boolean closeDrawer(View drawer) {
        return closeDrawer(drawer, true);
    }

    private boolean closeDrawerBookmarks() {
        View drawer = (View)drawer_left_bookmarks_listView;
        return closeDrawer(drawer);
    }

    private boolean closeDrawerVideos() {
        View drawer = (View)drawer_right_videos_listView;
        return closeDrawer(drawer);
    }

    // ---------------------------------------------------------------------------------------------
    // WebView:
    // ---------------------------------------------------------------------------------------------

    private void initWebView() {
        webViewClient = new BrowserWebViewClient(BrowserActivity.this) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                updateCurrentPage(url, false);
                resetSavedVideos();
                invalidateOptionsMenu();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                invalidateOptionsMenu();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                progressBar.setVisibility(View.GONE);
                invalidateOptionsMenu();
            }
        };

        downloadListener = new BrowserDownloadListener(webViewClient);

        webView.setWebViewClient(webViewClient);
        webView.setDownloadListener(downloadListener);

        WebSettings webSettings = webView.getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUserAgentString(
            getResources().getString(R.string.user_agent)
        );
        if (Build.VERSION.SDK_INT >= 17) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setInitialScale(0);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.clearCache(true);
        webView.clearHistory();
    }

    private void updateCurrentPage(String uri, boolean loadUrl) {
        current_page_url = uri;
        search.setQueryHint(current_page_url);
        search.setQuery(current_page_url, false);

        if (loadUrl) {
            webView.loadUrl(current_page_url);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Intent:
    // ---------------------------------------------------------------------------------------------

    private void openVideos(ArrayList<String> arrSources) {
        String jsonSources = new Gson().toJson(arrSources);
        Intent in = new Intent(BrowserActivity.this, com.github.warren_bank.webcast.exoplayer2.VideoActivity.class);

        in.putExtra("video_sources", jsonSources);
        startActivity(in);
    }

    private void openVideo(DrawerListItem item) {
        if (item == null) return;

        ArrayList<String> arrSources = new ArrayList<String>(3);
        arrSources.add(item.uri);
        arrSources.add(item.mimeType);
        arrSources.add(item.referer);
        openVideos(arrSources);
    }

    private void openAllVideos() {
        int len = 3 * drawer_right_videos_arrayList.size();
        if (len == 0) return;

        ArrayList<String> arrSources = new ArrayList<String>(len);
        int i;
        DrawerListItem item;
        for (i=0; i < drawer_right_videos_arrayList.size(); i++) {
            item = drawer_right_videos_arrayList.get(i);
            arrSources.add(item.uri);
            arrSources.add(item.mimeType);
            arrSources.add(item.referer);
        }
        openVideos(arrSources);
    }

    // ---------------------------------------------------------------------------------------------
    // ActionBar:
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browser, menu);

        if (drawer_left_bookmarks_arrayList.size() > 0) {
            if (DrawerListItem.contains(drawer_left_bookmarks_arrayList, current_page_url)) {
                menu.getItem(0).setIcon(R.drawable.ic_bookmark_saved);
            } else {
                menu.getItem(0).setIcon(R.drawable.ic_bookmark_unsaved);
            }
        }
        else {
            menu.getItem(0).setIcon(R.drawable.ic_bookmark_unsaved);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        switch(menuItem.getItemId()) {

            case R.id.action_bookmark: {
                DrawerListItem item = new DrawerListItem(
                    /* uri=      */ current_page_url,
                    /* title=    */ webView.getTitle().trim(),
                    /* mimeType= */ null,
                    /* referer=  */ webView.getUrl()
                );
                toggleSavedBookmark(item);
                return true;
            }

            case R.id.action_bookmarks: {
                toggleDrawerBookmarks();
                return true;
            }

            case R.id.action_videos: {
                toggleDrawerVideos();
                return true;
            }

            case R.id.action_exit: {
                finish();
                return true;
            }

            default: {
                return super.onOptionsItemSelected(menuItem);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
}
