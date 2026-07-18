package com.merg.quoteapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.AuthState;
import com.merg.quoteapp.ui.profile.LegalDocumentActivity;
import com.merg.quoteapp.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private MaterialButton registerButton;
    private TextInputEditText usernameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialCheckBox termsCheckBox;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameInput = findViewById(R.id.editRegisterUsername);
        emailInput = findViewById(R.id.editRegisterEmail);
        passwordInput = findViewById(R.id.editRegisterPassword);
        confirmPasswordInput = findViewById(R.id.editRegisterConfirmPassword);
        termsCheckBox = findViewById(R.id.checkRegisterTerms);
        registerButton = findViewById(R.id.buttonRegister);
        statusText = findViewById(R.id.textRegisterStatus);

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getRegisterState().observe(this, this::renderRegisterState);

        registerButton.setOnClickListener(view -> {
            if (!termsCheckBox.isChecked()) {
                showStatus(getString(R.string.register_terms_required), true);
                return;
            }
            viewModel.register(
                    textOf(usernameInput),
                    textOf(emailInput),
                    textOf(passwordInput),
                    textOf(confirmPasswordInput)
            );
        });

        findViewById(R.id.buttonBackToLogin).setOnClickListener(view -> finish());
        findViewById(R.id.linkRegisterPrivacy).setOnClickListener(view ->
                openLegalDocument(R.string.settings_privacy_policy, R.raw.legal_privacy_policy_tr));
        findViewById(R.id.linkRegisterKvkk).setOnClickListener(view ->
                openLegalDocument(R.string.legal_kvkk_title, R.raw.legal_kvkk_tr));
        findViewById(R.id.linkRegisterTerms).setOnClickListener(view ->
                openLegalDocument(R.string.settings_terms, R.raw.legal_terms_tr));
    }

    private void renderRegisterState(AuthState state) {
        boolean loading = state.getStatus() == AuthState.Status.LOADING;
        registerButton.setEnabled(!loading);
        usernameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmPasswordInput.setEnabled(!loading);
        termsCheckBox.setEnabled(!loading);

        if (loading) {
            hideStatus();
        } else if (state.getStatus() == AuthState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else if (state.getStatus() == AuthState.Status.SUCCESS) {
            showStatus(getString(R.string.register_success), false);
            statusText.postDelayed(this::openMain, 500L);
        }
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    private void openLegalDocument(int titleRes, int rawResId) {
        Intent intent = new Intent(this, LegalDocumentActivity.class);
        intent.putExtra(LegalDocumentActivity.EXTRA_TITLE, getString(titleRes));
        intent.putExtra(LegalDocumentActivity.EXTRA_RAW_RES_ID, rawResId);
        startActivity(intent);
    }
}
