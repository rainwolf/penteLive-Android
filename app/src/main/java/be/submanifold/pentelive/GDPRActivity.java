package be.submanifold.pentelive;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.Button;

public class GDPRActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdpr);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitle(getString(R.string.terms_conditions));
        setSupportActionBar(myToolbar);

        findViewById(R.id.privacyButton).setOnClickListener(view -> {
            LinkOpener.openExternal(this, "https://www.pente.org/help/helpWindow.jsp?file=privacyPolicy");
        });
        findViewById(R.id.acceptTermsButton).setOnClickListener(view -> {
            PrefUtils.saveBooleanToPrefs(GDPRActivity.this, PrefUtils.PREFS_GDPR_KEY, true);
            finish();
        });
        findViewById(R.id.rejectTermsButton).setOnClickListener(view -> {
            MyApplication.setShouldQuit(true);
            finish();
        });
    }

}
