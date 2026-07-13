package com.merg.quoteapp.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    private AuthViewModel viewModel;
    private String originalButtonText;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.editResetEmail);
        sendButton = findViewById(R.id.buttonSendResetLink);
        statusText = findViewById(R.id.textResetStatus);
        cooldownManager = new PasswordResetCooldownManager(this);

        originalButtonText = sendButton.getText().toString();
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
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
        if (state == null) {
            loading = false;
            updateCooldownState();
            return;
        }
        loading = state.getStatus() == AuthState.Status.LOADING;
        sendButton.setEnabled(!loading && !cooldownManager.isCoolingDown());
        emailInput.setEnabled(!loading);
        sendButton.setText(loading ? getString(R.string.sending) : originalButtonText);

        if (loading) {
            hideStatus();
        } else if (state.getStatus() == AuthState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else if (state.getStatus() == AuthState.Status.SUCCESS) {
            cancelCooldownTimer();
            cooldownManager.startCooldown();
            showSuccessFeedback();
            updateCooldownState();
            viewModel.clearResetState();
        }
    }

    private void updateCooldownState() {
        long remaining = cooldownManager.remainingSeconds();
        if (remaining <= 0) {
            cancelCooldownTimer();
            sendButton.setEnabled(!loading);
            sendButton.setText(originalButtonText);
            hideStatus();
            return;
        }
        showCooldown(remaining);
        cancelCooldownTimer();
        cooldownTimer = new CountDownTimer(remaining * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1L, (millisUntilFinished + 999L) / 1000L);
                showCooldown(seconds);
            }

            @Override
            public void onFinish() {
                cooldownManager.clearCooldown();
                sendButton.setEnabled(!loading);
                sendButton.setText(originalButtonText);
                hideStatus();
            }
        }.start();
    }

    private void showCooldown(long remaining) {
        sendButton.setEnabled(false);
        sendButton.setText(originalButtonText);
        if (remaining > 0) {
            showStatus(getString(R.string.reset_cooldown_format, remaining), false);
        }
    }

    private void showCooldown() {
        long remaining = cooldownManager.remainingSeconds();
        if (remaining <= 0) {
            updateCooldownState();
            return;
        }
        showCooldown(remaining);
    }

    private void showSuccessFeedback() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_success_feedback_title)
                .setMessage(R.string.reset_success_feedback_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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

    private void cancelCooldownTimer() {
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelCooldownTimer();
        super.onDestroy();
    }
}
