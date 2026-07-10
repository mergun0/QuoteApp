package com.merg.quoteapp.ui.profile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.merg.quoteapp.R;
import com.merg.quoteapp.repository.AuthRepository;
import com.merg.quoteapp.ui.auth.LoginActivity;

public class SettingsActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        authRepository = AuthRepository.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(view -> finish());

        setupRows();
    }

    private void setupRows() {
        LinearLayout accountContainer = findViewById(R.id.containerSettingsAccount);
        LinearLayout appearanceContainer = findViewById(R.id.containerSettingsAppearance);
        LinearLayout privacyContainer = findViewById(R.id.containerSettingsPrivacy);
        LinearLayout applicationContainer = findViewById(R.id.containerSettingsApplication);

        addRow(accountContainer, "👤", getString(R.string.settings_account_info),
                getString(R.string.settings_account_info_subtitle), "›",
                view -> showAccountInfo());
        addRow(accountContainer, "🔐", getString(R.string.settings_reset_password),
                getString(R.string.settings_reset_password_subtitle), "›",
                view -> sendPasswordReset());
        addRow(accountContainer, "🚪", getString(R.string.logout),
                getString(R.string.settings_logout_subtitle), "›",
                view -> logout());

        addRow(appearanceContainer, "🌙", getString(R.string.settings_theme),
                getString(R.string.settings_dark_theme_soon), getString(R.string.coming_soon),
                null);

        addRow(privacyContainer, "🛡️", getString(R.string.settings_privacy_policy),
                getString(R.string.settings_legal_placeholder_subtitle), "›",
                view -> showLegalPlaceholder(R.string.settings_privacy_policy,
                        R.string.settings_privacy_placeholder_message));
        addRow(privacyContainer, "📄", getString(R.string.settings_terms),
                getString(R.string.settings_legal_placeholder_subtitle), "›",
                view -> showLegalPlaceholder(R.string.settings_terms,
                        R.string.settings_terms_placeholder_message));
        addRow(privacyContainer, "🚫", getString(R.string.settings_blocked_users),
                getString(R.string.settings_blocked_users_subtitle), getString(R.string.coming_soon),
                null);

        addRow(applicationContainer, "ℹ️", getString(R.string.about_title),
                getString(R.string.settings_about_subtitle), "›",
                view -> openAbout());
        addRow(applicationContainer, "🏷️", getString(R.string.settings_version),
                getString(R.string.settings_version_format, appVersionName()), "",
                null);
        addRow(applicationContainer, "✉️", getString(R.string.settings_feedback),
                getString(R.string.settings_feedback_subtitle), "›",
                view -> sendFeedback());
    }

    private void addRow(LinearLayout container, String icon, String title,
                        String subtitle, String indicator, View.OnClickListener listener) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_settings_row, container, false);
        TextView iconText = row.findViewById(R.id.textSettingsRowIcon);
        TextView titleText = row.findViewById(R.id.textSettingsRowTitle);
        TextView subtitleText = row.findViewById(R.id.textSettingsRowSubtitle);
        TextView indicatorText = row.findViewById(R.id.textSettingsRowIndicator);

        iconText.setText(icon);
        titleText.setText(title);
        subtitleText.setText(subtitle);
        indicatorText.setText(indicator);
        if (listener == null) {
            row.setClickable(false);
            row.setFocusable(false);
        } else {
            row.setOnClickListener(listener);
        }
        container.addView(row);
    }

    private void showAccountInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user == null || isBlank(user.getEmail())
                ? getString(R.string.settings_account_email_missing)
                : user.getEmail();
        String username = user == null || isBlank(user.getDisplayName())
                ? safeUsernameFromEmail(email)
                : user.getDisplayName();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_account_info)
                .setMessage(getString(R.string.settings_account_dialog_format, username, email))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void sendPasswordReset() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || isBlank(user.getEmail())) {
            showMessage(R.string.settings_reset_password, R.string.settings_reset_email_missing);
            return;
        }
        authRepository.resetPassword(user.getEmail(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                showMessage(R.string.settings_reset_password, R.string.settings_reset_sent);
            }

            @Override
            public void onError(String message) {
                new MaterialAlertDialogBuilder(SettingsActivity.this)
                        .setTitle(R.string.settings_reset_password)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    private void showLegalPlaceholder(int titleRes, int messageRes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void sendFeedback() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.settings_feedback_email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_feedback_subject));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.settings_feedback)));
        } catch (ActivityNotFoundException error) {
            showMessage(R.string.settings_feedback, R.string.settings_feedback_no_app);
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String appVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception error) {
            return "1.0";
        }
    }

    private void showMessage(int titleRes, int messageRes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String safeUsernameFromEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return getString(R.string.username);
        }
        return email.substring(0, email.indexOf('@'));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
