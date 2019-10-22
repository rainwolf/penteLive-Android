package be.submanifold.pentelive;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.ads.MobileAds;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileAds.initialize(this, "ca-app-pub-3326997956703582~7930084241");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MyApplication.shouldQuit()) {
            MyApplication.setShouldQuit(false);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                finishAndRemoveTask();
//            } else {
//                finish();
//            }
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
