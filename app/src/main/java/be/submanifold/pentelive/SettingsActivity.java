package be.submanifold.pentelive;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle("Settings");
        setSupportActionBar(myToolbar);


        ((ToggleButton) findViewById(R.id.avatarToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(SettingsActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, false));
        ((ToggleButton) findViewById(R.id.avatarToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            PrefUtils.saveBooleanToPrefs(SettingsActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, isChecked);
            if (!isChecked) {
                PentePlayer.avatars.clear();
                PentePlayer.pendingAvatarChecks.clear();
            }
            }
        });
    }
}
