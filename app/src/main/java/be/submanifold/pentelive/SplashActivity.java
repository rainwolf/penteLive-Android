package be.submanifold.pentelive;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class SplashActivity extends AppCompatActivity {

    public static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() { @Override public boolean verify(String hostname, SSLSession session) { return true; } });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (PentePlayer.development) {
            disableSSLCertificateChecking();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MyApplication.shouldQuit()) {
            MyApplication.setShouldQuit(false);
            finish();
        } else {
            boolean gdprAccepted = PrefUtils.getBooleanFromPrefs(SplashActivity.this, PrefUtils.PREFS_GDPR_KEY, false);
            if (!gdprAccepted) {
                Intent intent = new Intent(this, GDPRActivity.class);
//        startActivityForResult(intent, 3);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, LoginActivity.class);
//        startActivityForResult(intent, 3);
                startActivity(intent);
//        finish();
            }
        }
    }

//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        System.out.println("onActivityResult " + requestCode + " " + data.getData().toString());
//        if (requestCode == 3) {
//            if (resultCode == RESULT_OK) {
//                String returnedResult = data.getData().toString();
//                if (returnedResult != null && returnedResult.contains("backPressed")) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        finishAndRemoveTask();
//                    }
//                }
//            }
//        }
//    }

}
