package be.submanifold.pentelive.net;

import android.webkit.CookieManager;

/**
 * {@link CookieStorage} on the WebView cookie store ({@link CookieManager}), so WebView pages
 * share the session and login cookies, and the cookies persist across process restarts
 * (WebView keeps them in app_webview/). The CookieManager is looked up on each call, so
 * WebView is only initialised by the first request that needs a cookie.
 */
public final class WebkitCookieStorage implements CookieStorage {

    @Override
    public String cookieHeader(String url) {
        String cookies = CookieManager.getInstance().getCookie(url);
        // getCookie documents null as "no cookies for this URL"; it is not a failure.
        return cookies == null ? "" : cookies;
    }

    @Override
    public void setCookie(String url, String setCookieValue) {
        CookieManager.getInstance().setCookie(url, setCookieValue);
    }

    @Override
    public void removeAll() {
        // Asynchronous, but ordered before later cookie calls on CookieManager's own task
        // runner. AuthedHttp.loginFresh checks afterwards that the login cookies are gone.
        CookieManager.getInstance().removeAllCookies(null);
    }

    @Override
    public void flush() {
        CookieManager.getInstance().flush();
    }
}
