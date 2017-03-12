package be.submanifold.pentelive;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            Intent intent = new Intent(this, LoginActivity.class);
//        startActivityForResult(intent, 3);
            startActivity(intent);
//        finish();
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
