package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewActivity extends AppCompatActivity {

    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            webview = findViewById(R.id.webview);
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
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
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setBuiltInZoomControls(true);
            webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

//            cookieMap.put("Cookie", "name2="+PentePlayer.mPlayerName+"; password2="+PentePlayer.mPassword);

            webview.getSettings().setLoadWithOverviewMode(true);
            webview.getSettings().setUseWideViewPort(true);
            String urlStr = extras.getString("url");
            if (urlStr.contains("//pente.org")) {
//                System.out.println(urlStr);
                urlStr = urlStr.replace("//pente.org", "//www.pente.org");
//                System.out.println(urlStr);
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

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
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
