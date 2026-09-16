package src.comitton.config;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import src.comitton.common.SystemUiHelper;

public abstract class BasePreferenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUiHelper.setupEdgeToEdge(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, createPreferenceFragment())
                    .commit();
        }

        android.view.View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            SystemUiHelper.applySystemWindowInsets(contentView, true, true, true, true);
        }
    }

    protected abstract PreferenceFragmentCompat createPreferenceFragment();
}
