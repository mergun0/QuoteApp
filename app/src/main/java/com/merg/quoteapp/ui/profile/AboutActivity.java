package com.merg.quoteapp.ui.profile;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.merg.quoteapp.R;
import com.merg.quoteapp.utils.AccountDeletionGuard;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        AccountDeletionGuard.enforce(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAbout);
        toolbar.setNavigationOnClickListener(view -> finish());

        TextView versionText = findViewById(R.id.textAboutVersion);
        versionText.setText(getString(R.string.about_version_status_format,
                appVersionName(), getString(R.string.about_status_beta)));
    }

    private String appVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception error) {
            return "1.0";
        }
    }
}
