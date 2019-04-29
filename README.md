#### [WebCast](https://github.com/warren-bank/Android-WebCast/tree/04-webcast-filename):

#### Methodology:

_previous branches_

* used an external HTTP client library
* used the `Content-Type` response header to detect video files

_now_

* uses WebView to download all HTTP requests
* uses regular expressions to detect file extensions associated with video formats in URL requests

#### Notes:

_relevant source code:_

* [BrowserWebViewClient](src/main/java/com/github/warren_bank/webcast/webview/BrowserWebViewClient.java)
  * [regex to detect video files](https://github.com/warren-bank/crx-webcast-reloaded/blob/gh-pages/chrome_extension/background.js#L2)

_references:_

* ["WebCast-Reloaded" Chromium extension](https://github.com/warren-bank/crx-webcast-reloaded)

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
