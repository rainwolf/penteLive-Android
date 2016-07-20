package be.submanifold.pentelive;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.CookieHandler;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.webkit.CookieSyncManager webCookieSync =
                CookieSyncManager.createInstance(this);
        android.webkit.CookieManager webCookieManager =
                CookieManager.getInstance();
        webCookieManager.setAcceptCookie(true);

        // Get cookie manager for HttpURLConnection
        java.net.CookieStore rawCookieStore = ((java.net.CookieManager)
                CookieHandler.getDefault()).getCookieStore();

        // Construct URI
        java.net.URI baseUri = null;
        try {
            baseUri = new URI("https://www.pente.org");
        } catch (URISyntaxException e) {
            // Handle invalid URI
        }

        // Copy cookies from HttpURLConnection to WebView
        List<HttpCookie> cookies = rawCookieStore.get(baseUri);
        String url = baseUri.toString();
        for (HttpCookie cookie : cookies) {
            String setCookie = new StringBuilder(cookie.toString())
                    .append("; domain=").append(cookie.getDomain())
                    .append("; path=").append(cookie.getPath())
                    .toString();
            webCookieManager.setCookie(url, setCookie);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            WebView webview = (WebView) findViewById(R.id.webview);
            webview.setWebViewClient(new WebViewClient()
            {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    view.loadUrl(url);
//                    System.out.println("hello " + url);
                    return true;
                }
            });
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setBuiltInZoomControls(true);
            webview.getSettings().setLoadWithOverviewMode(true);
            webview.getSettings().setUseWideViewPort(true);
            String urlStr = extras.getString("url");
            webview.loadUrl(urlStr);
//                    System.out.println("hello " + url);
        }

    }
}
