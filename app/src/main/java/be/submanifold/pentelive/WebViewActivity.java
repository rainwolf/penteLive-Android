package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import be.submanifold.pentelive.billing.SubscriptionLinks;
import be.submanifold.pentelive.net.StaticBaseUrlProvider;

public class WebViewActivity extends AppCompatActivity {

    private WebView webview;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** Set when a blocked request opened StoreActivity; cleared on resume so a later one opens it again. */
    private final AtomicBoolean storeOpened = new AtomicBoolean();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            webview = findViewById(R.id.webview);
            webview.setWebViewClient(new WebViewClient() {
                /**
                 * Also sees POST navigations, which skip shouldOverrideUrlLoading (the profile
                 * page's gift form, PayPal's buy buttons). Runs on a WebView background thread.
                 */
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    if (request.isForMainFrame() && !SubscriptionLinks.isPayPal(url)
                            && LinkOpener.decide(url) != LinkPolicy.Action.IN_APP) {
                        // PayPal falls through to the StoreActivity path below, never the browser.
                        // Covers POST/form navigations that skip shouldOverrideUrlLoading.
                        handOff(url);
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                    }
                    if (!SubscriptionLinks.isSubscriptionPage(url, serverHost()) && !SubscriptionLinks.isPayPal(url)) {
                        return super.shouldInterceptRequest(view, request);
                    }
                    if (request.isForMainFrame() && storeOpened.compareAndSet(false, true)) {
                        mainHandler.post(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                startActivity(new Intent(WebViewActivity.this, StoreActivity.class));
                            }
                        });
                    }
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    if (SubscriptionLinks.isPayPal(url)) {
                        // The app never opens PayPal; send the user to Play Billing instead.
                        startActivity(new Intent(WebViewActivity.this, StoreActivity.class));
                        return true;
                    }
                    if (LinkOpener.decide(url) != LinkPolicy.Action.IN_APP) {
                        // Subframes (ad iframes) only count when the user tapped; else let them load.
                        if (!request.isForMainFrame() && !request.hasGesture()) {
                            return false;
                        }
                        handOff(url);
                        return true;
                    }
                    if (SubscriptionLinks.isSubscriptionPage(url, serverHost())) {
                        startActivity(new Intent(WebViewActivity.this, StoreActivity.class));
                        return true;
                    }
                    if (url.contains("?mobile&g=") || url.contains("gameServer/tb/game?gid=")) {
                        if (url.contains("?mobile&g=")) {
                            while (!url.startsWith("?mobile&g=")) {
                                url = url.substring(1);
                            }
                            url = url.substring(10);
                        }
                        if (url.contains("gameServer/tb/game?gid=")) {
                            while (!url.startsWith("gameServer/tb/game?gid=")) {
                                url = url.substring(1);
                            }
                            url = url.substring(23);
                        }
                        for (int i = 0; i < url.length(); i++) {
                            if (url.charAt(i) < '0' || url.charAt(i) > '9') {
                                url = url.substring(0, i);
                                break;
                            }
                        }

                        Game game = new Game(url, null, null, null, null, null, null, null, null, null, null);
                        game.setActive(false);
                        Intent intent = new Intent(WebViewActivity.this, BoardActivity.class);
                        intent.putExtra("game", game);
                        startActivity(intent);

                        return true;
                    }
                    return false;
                }
            });
            webview.getSettings().setAllowFileAccess(false);
            webview.getSettings().setAllowContentAccess(false);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setBuiltInZoomControls(true);
            webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);


            webview.getSettings().setLoadWithOverviewMode(true);
            webview.getSettings().setUseWideViewPort(true);
            webview.getSettings().setUserAgentString(webview.getSettings().getUserAgentString() + " penteLive-Android");
            String urlStr = extras.getString("url");
            if (urlStr.contains("//pente.org")) {
//                System.out.println(urlStr);
                urlStr = urlStr.replace("//pente.org", "//www.pente.org");
//                System.out.println(urlStr);
            }
            if (SubscriptionLinks.isSubscriptionPage(urlStr, serverHost())) {
                startActivity(new Intent(this, StoreActivity.class));
                finish();
                return;
            }
            if (LinkOpener.decide(urlStr) != LinkPolicy.Action.IN_APP) {
                LinkOpener.openExternal(this, urlStr);
                finish();
                return;
            }
            webview.loadUrl(urlStr);
//                    System.out.println("hello " + url);

        }

        getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    // Back handling must go through OnBackPressedDispatcher: onBackPressed() is no
    // longer called for back gestures once the app targets API 36 (predictive back).
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (webview != null && webview.canGoBack()) {
                webview.goBack();
                return;
            }

            // Otherwise defer to system default behavior.
            setEnabled(false);
            getOnBackPressedDispatcher().onBackPressed();
            // Re-arm in case the re-dispatch did not finish the activity.
            setEnabled(true);
        }
    };

    /** A navigation the web view may not make: system browser, or blocked (kids, bad scheme). Any thread. */
    private void handOff(String url) {
        mainHandler.post(() -> {
            if (!isFinishing() && !isDestroyed() && LinkOpener.decide(url) != LinkPolicy.Action.BLOCKED) {
                LinkOpener.openExternal(this, url);
            }
        });
    }

    /** Host of the configured server: www.pente.org, or 10.0.2.2 in development builds. */
    private static String serverHost() {
        return URI.create(new StaticBaseUrlProvider(PentePlayer.development).baseUrl()).getHost();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        storeOpened.set(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    //This is the handler that will manager to process the broadcast intent
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("message");

            if (message != null && !message.isEmpty()) {
                Toast.makeText(WebViewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }
    };


}
