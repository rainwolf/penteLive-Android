package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.CookieHandler;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebViewActivity extends AppCompatActivity {

    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            webview = (WebView) findViewById(R.id.webview);
            webview.setWebViewClient(new WebViewClient()
            {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
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
                        for (int i = 0; i < url.length(); i++ ) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            }

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

    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
            return;
        }

        // Otherwise defer to system default behavior.
        super.onBackPressed();
    }
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
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("message");

            if (message!=null && message.length()>0) {
                Toast.makeText(WebViewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }
    };


}
