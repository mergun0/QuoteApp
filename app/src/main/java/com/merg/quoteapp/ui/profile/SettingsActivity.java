package com.merg.quoteapp.ui.profile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.merg.quoteapp.repository.ProfileRepository;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.utils.PasswordResetCooldownManager;

public class SettingsActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private ProfileRepository profileRepository;
    private PasswordResetCooldownManager cooldownManager;
    private CountDownTimer resetCooldownTimer;
    private View resetPasswordRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        authRepository = AuthRepository.getInstance();
        profileRepository = new ProfileRepository();
        cooldownManager = new PasswordResetCooldownManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(view -> finish());

        setupRows();
        updateResetCooldownState();
    }

    private void setupRows() {
        LinearLayout accountContainer = findViewById(R.id.containerSettingsAccount);
        LinearLayout appearanceContainer = findViewById(R.id.containerSettingsAppearance);
        LinearLayout privacyContainer = findViewById(R.id.containerSettingsPrivacy);
        LinearLayout applicationContainer = findViewById(R.id.containerSettingsApplication);

        addRow(accountContainer, "👤", getString(R.string.settings_account_info),
                getString(R.string.settings_account_info_subtitle), "›",
                view -> showAccountInfo());
        resetPasswordRow = addRow(accountContainer, "🔐", getString(R.string.settings_reset_password),
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

    private View addRow(LinearLayout container, String icon, String title,
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
        return row;
    }

    private void showAccountInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_account_info)
                    .setMessage(getString(R.string.settings_account_dialog_format,
                            getString(R.string.settings_account_username_fallback),
                            getString(R.string.settings_account_email_missing)))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_account_info)
                .setMessage(R.string.settings_account_loading)
                .setPositiveButton(android.R.string.ok, null);
        androidx.appcompat.app.AlertDialog dialog = builder.show();

        profileRepository.getAccountInfo(new ProfileRepository.AccountInfoCallback() {
            @Override
            public void onSuccess(String username, String email) {
                String safeUsername = isBlank(username)
                        ? getString(R.string.settings_account_username_fallback) : username;
                String safeEmail = isBlank(email)
                        ? getString(R.string.settings_account_email_missing) : email;
                dialog.setMessage(getString(R.string.settings_account_dialog_format,
                        safeUsername, safeEmail));
            }

            @Override
            public void onError(String message) {
                dialog.setMessage(getString(R.string.settings_account_dialog_format,
                        getString(R.string.settings_account_username_fallback),
                        user.getEmail() == null ? getString(R.string.settings_account_email_missing)
                                : user.getEmail()));
            }
        });
    }

    private void sendPasswordReset() {
        if (cooldownManager.isCoolingDown()) {
            showMessage(R.string.settings_reset_password,
                    getString(R.string.reset_cooldown_format, cooldownManager.remainingSeconds()));
            updateResetCooldownState();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || isBlank(user.getEmail())) {
            showMessage(R.string.settings_reset_password, R.string.settings_reset_email_missing);
            return;
        }
        authRepository.resetPassword(user.getEmail(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                cooldownManager.startCooldown();
                updateResetCooldownState();
                showMessage(R.string.settings_reset_password, R.string.reset_privacy_safe_success);
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

    private void showMessage(int titleRes, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void updateResetCooldownState() {
        if (resetPasswordRow == null) {
            return;
        }
        long remaining = cooldownManager.remainingSeconds();
        resetPasswordRow.setEnabled(remaining <= 0);
        resetPasswordRow.setAlpha(remaining <= 0 ? 1f : 0.65f);
        if (resetCooldownTimer != null) {
            resetCooldownTimer.cancel();
            resetCooldownTimer = null;
        }
        if (remaining > 0) {
            resetCooldownTimer = new CountDownTimer(remaining * 1000L, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    resetPasswordRow.setEnabled(false);
                }

                @Override
                public void onFinish() {
                    resetPasswordRow.setEnabled(true);
                    resetPasswordRow.setAlpha(1f);
                }
            }.start();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onDestroy() {
        if (resetCooldownTimer != null) {
            resetCooldownTimer.cancel();
        }
        super.onDestroy();
    }
}
