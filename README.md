#### [WebCast](https://github.com/warren-bank/Android-WebCast/tree/03-webcast-okhttp):

#### Notes - HTTP client:

_source code class consumer(s):_

* [BrowserWebViewClient](src/main/java/com/github/warren_bank/webcast/webview/BrowserWebViewClient.java)

_references:_

* https://github.com/square/okhttp/blob/master/CHANGELOG.md
* https://github.com/square/okhttp/blob/master/CHANGELOG.md#version-3130
  * OkHttp 3.13.0 bumps our minimum requirements to Java 8+ or Android 5+.
  * OkHttp 3.12.x branch will be our long-term branch for Android 2.3+ (API level 9+) and Java 7+.
  * We will backport critical fixes to the 3.12.x branch through December 31, 2020.
* https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
* https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp/3.12.2
  * tip of branch: 3.12.2
* https://guides.codepath.com/android/Using-OkHttp
* https://github.com/square/okhttp/wiki/Recipes
  * tutorials
* https://github.com/franmontiel/PersistentCookieJar
  * persistent cookie jar for OkHttp 3.x
* https://stackoverflow.com/a/37478166
  * nice, simple, non-persistent.. in-memory cookie jar w/ no extra dependencies
* https://stackoverflow.com/a/46997153
  * the "official" cookie jar implementation?
* https://gist.github.com/Jthomas54/2f34b8aea5b457db5459e2421deffd15
  * nice! shows how to use w/ "android.webkit.CookieManager"
  * benefit: all instances of WebView will share the same cookies as OkHttp client
* https://github.com/classycodeoss/nfc-sockets/blob/master/android/NFCSockets/app/src/main/java/com/classycode/nfcsockets/okhttp/OkHttpWebViewClient.java#L135
  * nice! shows exactly how to use OkHttp with "WebView.shouldInterceptRequest"
  * special attention: "okHttpResponseToWebResourceResponse"
* https://gist.github.com/kmerrell42/b4ff31733c562a3262ee9a42f5704a89
  * another example using OkHttp with "WebView.shouldInterceptRequest"

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
