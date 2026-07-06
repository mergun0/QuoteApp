package com.merg.quoteapp.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.AuthState;
import com.merg.quoteapp.viewmodel.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {

    private MaterialButton sendButton;
    private TextInputEditText emailInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.editResetEmail);
        sendButton = findViewById(R.id.buttonSendResetLink);
        statusText = findViewById(R.id.textResetStatus);

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getResetState().observe(this, this::renderResetState);

        sendButton.setOnClickListener(view ->
                viewModel.resetPassword(textOf(emailInput)));

        findViewById(R.id.buttonBackToLogin).setOnClickListener(view -> finish());
    }

    private void renderResetState(AuthState state) {
        boolean loading = state.getStatus() == AuthState.Status.LOADING;
        sendButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);

        if (loading) {
            hideStatus();
        } else if (state.getStatus() == AuthState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else if (state.getStatus() == AuthState.Status.SUCCESS) {
            showStatus(getString(R.string.reset_success), false);
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
}
