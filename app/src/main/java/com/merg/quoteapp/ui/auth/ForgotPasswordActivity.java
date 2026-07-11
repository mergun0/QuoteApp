package com.merg.quoteapp.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.AuthState;
import com.merg.quoteapp.utils.PasswordResetCooldownManager;
import com.merg.quoteapp.viewmodel.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {

    private MaterialButton sendButton;
    private TextInputEditText emailInput;
    private TextView statusText;
    private PasswordResetCooldownManager cooldownManager;
    private CountDownTimer cooldownTimer;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.editResetEmail);
        sendButton = findViewById(R.id.buttonSendResetLink);
        statusText = findViewById(R.id.textResetStatus);
        cooldownManager = new PasswordResetCooldownManager(this);

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getResetState().observe(this, this::renderResetState);

        sendButton.setOnClickListener(view -> {
            if (cooldownManager.isCoolingDown()) {
                showCooldown();
                return;
            }
            viewModel.resetPassword(textOf(emailInput));
        });

        findViewById(R.id.buttonBackToLogin).setOnClickListener(view -> finish());
        updateCooldownState();
    }

    private void renderResetState(AuthState state) {
        loading = state.getStatus() == AuthState.Status.LOADING;
        sendButton.setEnabled(!loading && !cooldownManager.isCoolingDown());
        emailInput.setEnabled(!loading);

        if (loading) {
            hideStatus();
        } else if (state.getStatus() == AuthState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else if (state.getStatus() == AuthState.Status.SUCCESS) {
            cooldownManager.startCooldown();
            showStatus(getString(R.string.reset_privacy_safe_success), false);
            updateCooldownState();
        }
    }

    private void updateCooldownState() {
        long remaining = cooldownManager.remainingSeconds();
        if (remaining <= 0) {
            sendButton.setEnabled(!loading);
            if (cooldownTimer != null) {
                cooldownTimer.cancel();
                cooldownTimer = null;
            }
            return;
        }
        showCooldown();
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
        }
        cooldownTimer = new CountDownTimer(remaining * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                showCooldown();
            }

            @Override
            public void onFinish() {
                sendButton.setEnabled(!loading);
            }
        }.start();
    }

    private void showCooldown() {
        long remaining = cooldownManager.remainingSeconds();
        sendButton.setEnabled(false);
        if (remaining > 0) {
            showStatus(getString(R.string.reset_cooldown_format, remaining), false);
        }
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message);
        statusText.setTextColor(getColor(isError
                ? R.color.quote_status_error : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusText.setText("");
        statusText.setVisibility(View.GONE);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    @Override
    protected void onDestroy() {
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
        }
        super.onDestroy();
    }
}
