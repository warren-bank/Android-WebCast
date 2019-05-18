### [WebCast](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename)

Android app to watch web videos on Google Chromecast.

#### Description

App consists of 2 Activities:

1. WebView to browse websites
   * left drawer contains a list of _bookmarks_
     * data is persistent
     * click to:
       * open website URLs in WebView
       * watch video URLs in next Activity
     * long click to:
       * rename
       * delete
   * right drawer contains _videos_
     * data is transient and only valid for the current website
     * click to:
       * add to _bookmarks_
       * watch video URL in next Activity
     * long click to:
       * delete
   * menu icons:
     * _bookmarks_ toggle: add/remove current website URL
2. ExoPlayer to watch video(s)
   * video player w/:
     * cast icon when at least one Google Chromecast is detected on LAN
     * playlist
   * click on video URL in playlist to:
     * if not casting, load video in player
     * if casting, cast video to Google Chromecast (receiver app)

#### Notes

* when video is played in ExoPlayer
  * `Referer` HTTP header is set to the URL of the website
  * icon in lower right corner of toolbar to toggle fullscreen mode
* when video is cast to Google Chromecast
  * the video URL is communicated to the receiver app running in the Google Chromecast
  * the receiver app requests the video file/stream directly from its server
    * `Referer` HTTP header cannot be set to the URL of the website
      * the receiver app is restrained by standard browser security policies
        * the specs for [XHR](https://xhr.spec.whatwg.org/#dom-xmlhttprequest-setrequestheader) and [fetch](https://fetch.spec.whatwg.org/#forbidden-header-name) forbid changing certain HTTP request headers, including `Referer`
  * the Android device running this app can be turned off, and it would have no effect on playback of the video that is casting

- - - -

#### Status

* everything works, but..
  * how well will depend entirely upon the implementation of `WebView` on your Android device
    * this app supports Android 4.1 Jellybean and newer
      * Android 4.x
        * `WebView` is embedded into the Android operating system
        * `WebView` in 4.1 cannot load the javascript video player on many websites
          * consequently, the video URL is never requested.. and this app has no opportunity to intercept it
        * `WebView` in 4.4 is also imperfect, though noticeably better than in Android 4.1
      * Android 5.0+
        * `WebView` in 5.0 was moved to a system app
          * [upgradable](https://play.google.com/store/apps/details?id=com.google.android.webview)
          * based on the Google Chrome web browser
          * works __much__ better than earlier embedded implementations
  * the ExoPlayer Chromecast extension could be better
    * future updates to this library would directly benefit future builds of this app

#### TL;DR

* for best results, if possible, please update your [_Android System WebView_](https://play.google.com/store/apps/details?id=com.google.android.webview)

- - - -

#### Organization of Git Repo:

_methodology used in previous branches_

* used an external HTTP client library
* used the `Content-Type` response header to detect video files

_methodology used in this branch_

* uses WebView to download all HTTP requests
* uses regular expressions to detect file extensions associated with video formats in URL requests

#### Highlights of Source Code:

_identification of video URLs in outbound HTTP requests:_

* [BrowserWebViewClient](WebCast/src/main/java/com/github/warren_bank/webcast/webview/BrowserWebViewClient.java)
  * [regex to detect video files](https://github.com/warren-bank/Android-WebCast/blob/04-webcast-filename/WebCast/src/main/java/com/github/warren_bank/webcast/webview/BrowserWebViewClient.java#L96)

_same methodology as applied to a Chrome extension:_

* ["WebCast-Reloaded" Chromium extension](https://github.com/warren-bank/crx-webcast-reloaded)
  * [regex to detect video files](https://github.com/warren-bank/crx-webcast-reloaded/blob/gh-pages/chrome_extension/background.js#L2)

- - - -

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
