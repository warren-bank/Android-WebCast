### [WebCast](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename)

Android app to extract video (file/stream) URLs from websites and watch them elsewhere (internal/external video player, Google Chromecast, [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)).

- - - -

#### Screenshots

![WebCast](./screenshots/01-BrowserActivity-blank.png)
![WebCast](./screenshots/02-BrowserActivity-leftdrawer-bookmarks.png)
![WebCast](./screenshots/03-BrowserActivity-rightdrawer-videos.png)
![WebCast](./screenshots/04-BrowserActivity-menu.png)
![WebCast](./screenshots/05-SettingsActivity-preferences.png)
![WebCast](./screenshots/06-SettingsActivity-videoplayer.png)
![WebCast](./screenshots/07-BrowserActivity-leftdrawer-open-bookmark.png)
![WebCast](./screenshots/08-BrowserActivity-ABCNews.png)
![WebCast](./screenshots/09-BrowserActivity-ABCNews-rightdrawer-videos.png)
![WebCast](./screenshots/10-BrowserActivity-ABCNews-rightdrawer-open-video.png)
![WebCast](./screenshots/11-VideoActivity-internalvideoplayer-ABCNews.png)
![WebCast](./screenshots/12-VideoActivity-internalvideoplayer-ABCNews-landscape-fullscreen.png)
![WebCast](./screenshots/13-VideoActivity-internalvideoplayer-ABCNews-chromecast-devicelist.png)
![WebCast](./screenshots/14-VideoActivity-internalvideoplayer-ABCNews-chromecast-connected-casting.png)
![WebCast](./screenshots/15-Android-System-MediaRouter.png)
![WebCast](./screenshots/16-BrowserActivity-externalvideoplayer-implicit-intent-chooser.png)
![WebCast](./screenshots/17-ExoAirPlayerSenderActivity-landscape-zoom.png)
![WebCast](./screenshots/18-ExoAirPlayerSenderActivity.png)

- - - -

#### Tour

* `BrowserActivity` is shown when the app is started, and includes:
  - a very basic web browser
    * address bar
    * `WebView`
  - actionbar icons:
    * _bookmark_ toggle
      - add/remove current website URL to/from list of persistently saved __Bookmarks__
  - actionbar menu items:
    * _Bookmarks_
      - open drawer on left: __Bookmarks__
    * _Videos_
      - open drawer on right: __Videos__
    * _Settings_
      - open `SettingsActivity`
    * _Exit_
      - close all UI and exit the app
  - drawer on left: __Bookmarks__
    * contains a persistent list of:
      - website URLs that have been saved via the _bookmark_ toggle icon
      - video URLs that have been saved via the __Videos__ drawer
    * click a list item to:
      - open website URL in WebView
      - watch video URL
        * `SettingsActivity` determines the particular action to be performed
    * long click a list item to:
      - rename
      - delete
  - drawer on right: __Videos__
    * contains a transient list of video URLs that have been found on the web page that is currently loaded in the `WebView`
      - this list is cleared each time the `WebView` navigates to a new web page
    * click a list item to:
      - add video URL to list of persistently saved __Bookmarks__
      - watch video URL
        * `SettingsActivity` determines the particular action to be performed
    * long click a list item to:
      - delete
* `SettingsActivity` is started from the actionbar menu in `BrowserActivity`, and includes:
  - _Video Player_ to select whether to watch videos using..
    * internal w/ Chromecast sender
      - start `VideoActivity`
    * external
      - start Activity chooser w/ an implicit Intent
        * action
          - `android.intent.action.VIEW`
        * data
          - video URL
        * type
          - mime-type for format of video
        * extras
          - `referUrl`
            * (String) referer URL
            * used by [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
    * [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver) sender
      - start `ExoAirPlayerSenderActivity`
* `VideoActivity` is started when a video URL is watched in the internal video player, and includes:
  - [ExoPlayer](https://github.com/google/ExoPlayer/tree/release-v2)
    * displays an icon in lower right corner of video controls toolbar to toggle fullscreen mode on/off
  - [Chromecast sender](https://github.com/google/ExoPlayer/tree/release-v2/extensions/cast)
    * displays an actionbar _cast_ icon when at least one Google Chromecast is detected on LAN
    * when connected to a Chromecast
      - video URLs are communicated to the receiver app running in the Chromecast
      - the Chromecast receiver app loads the video URL in an embedded HTML5 video player
        * transfer of video data occurs directly between the Chromecast and the server that hosts the video URL
        * transfer would not be effected by any of the following events:
          - `VideoActivity` stopped
          - `BrowserActivity` stopped
          - [WebCast](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename) app exited
          - Android device powered off
  - list of video URLs
    * click a list item to:
      - play video URL
        * if connected to a Chromecast:
          - on Chromecast
        * otherwise:
          - on Android, in _ExoPlayer_
            * all HTTP requests include the referer url
* `ExoAirPlayerSenderActivity` is started when a video URL is watched in the [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver) sender, and includes:
  - `WebView` that loads a single web page
    * URL of the web page depends on version of Android
      - Android 5.0 and newer
        * [version using ES6+ modern javascript](http://webcast-reloaded.surge.sh/airplay_sender.html)
      - Android 4.x and older
        * [version using ES5 compliant javascript](http://webcast-reloaded.surge.sh/airplay_sender.es5.html)
    * URL hash contains:
      - `#/watch/${base64_video}/referer/${base64_referer}`
  - web page reads data from URL hash and pre-populates fields:
    * video url
    * referer url
  - web page reads data from cookies and pre-populates fields:
    * host
    * port
    * https
  - provides a basic UI to control any [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver) receiver app that is reachable through the network

- - - -

#### Important Caveats

* some video URLs may play in [WebCast](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename) and [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver), but cannot play on Chromecast or other external video players
  - this can occur when a video URL is hosted by a server that uses the `Referer` HTTP request header to restrict access, which is a common strategy
    * [WebCast](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename) and [ExoAirPlayer](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver) have the functionality to configure the value of this header for each unique video URL
    * Chromecast receiver apps cannot change the value of this header because they are restrained by standard browser security policies
      - the specs for [XHR](https://xhr.spec.whatwg.org/#dom-xmlhttprequest-setrequestheader) and [fetch](https://fetch.spec.whatwg.org/#forbidden-header-name) forbid changing certain HTTP request headers, including `Referer`
      - the [WebCast Chromecast receiver app](https://github.com/warren-bank/Android-WebCast/tree/05-chromecast-receiver-app)
        * [attempts](https://github.com/warren-bank/Android-WebCast/blob/05-chromecast-receiver-app/CastReceiver/js/receiver.js#L42) to change the value of this header
        * [reveals](https://github.com/warren-bank/Android-WebCast/blob/05-chromecast-receiver-app/notes.txt#L122) in the remote debugger console that this attempt raises the warning:
          - _Refused to set unsafe header "referer"_
    * other external video players would need to:
      - read the `referUrl` extra in the starting Intent
      - configure its HTTP client library to change the value of this header
* the Android System [`WebView`](https://developer.chrome.com/multidevice/webview/overview) component is wholly responsible for the web browser experience
  - this component has a complicated history
  - without going into detail:
    * on versions of Android &lt; 5.0
      - the `WebView` component is baked into the firmware
        * cannot be updated
        * does a poor job loading modern webpages, as the javascript language (ES6+) and html spec (HTML5) have changed significantly
    * on versions of [Android &gt;= 5.0](https://developer.android.com/about/versions/lollipop#WebView)
      - the `WebView` component is a [standalone application](https://play.google.com/store/apps/details?id=com.google.android.webview)
        * can be updated
    * on versions of [Android &gt;= 7.0](https://developer.android.com/about/versions/nougat/android-7.0#webview)
      - the `WebView` component is superseded by a component of the [Google Chrome web browser](https://play.google.com/store/apps/details?id=com.android.chrome), when it is installed and enabled
        * can be updated

- - - -

#### Organization of Git Repo

* __stale branches__
  - [01-foundation](https://github.com/warren-bank/Android-WebCast/tree/01-foundation)
    * collection of small apps to develop and test experimental features
  - [02-webcast-httpclient](https://github.com/warren-bank/Android-WebCast/tree/02-webcast-httpclient)
    * used an external HTTP client library: [Apache HttpClient](https://hc.apache.org/httpcomponents-client-4.3.x/index.html)
    * used the `Content-Type` response header to detect video files
  - [03-webcast-okhttp](https://github.com/warren-bank/Android-WebCast/tree/03-webcast-okhttp)
    * used an external HTTP client library: [okhttp](https://github.com/square/okhttp)
    * used the `Content-Type` response header to detect video files
* __active branches__
  - [04-webcast-filename](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename)
    * uses `WebView` to download all HTTP requests
    * uses regular expressions to detect file extensions associated with video formats in URL requests
  - [05-chromecast-receiver-app](https://github.com/warren-bank/Android-WebCast/tree/05-chromecast-receiver-app)
    * WebCast Chromecast receiver app
  - [gh-pages](https://github.com/warren-bank/Android-WebCast/tree/gh-pages)
    * WebCast Chromecast receiver app (mirror)
    * [hosted](https://warren-bank.github.io/Android-WebCast/CastReceiver/receiver.html) by [GitHub Pages](https://pages.github.com/)

#### Highlights of Source Code

* __identification of video URLs in outbound HTTP requests__
  - `BrowserWebViewClient`
    * [regex to detect video files](https://github.com/warren-bank/Android-WebCast/blob/04-webcast-filename/android-studio-project/WebCast/src/main/java/com/github/warren_bank/webcast/webview/BrowserWebViewClient.java#L96)
* __same methodology as applied to a Chrome extension__
  - ["WebCast-Reloaded" Chromium extension](https://github.com/warren-bank/crx-webcast-reloaded)
    * [regex to detect video files](https://github.com/warren-bank/crx-webcast-reloaded/blob/gh-pages/chrome_extension/background.js#L2)

- - - -

#### Legal

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
