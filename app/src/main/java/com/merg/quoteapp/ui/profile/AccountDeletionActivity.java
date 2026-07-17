package com.merg.quoteapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.R;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.viewmodel.AccountDeletionViewModel;

public class AccountDeletionActivity extends AppCompatActivity {

    public static final String EXTRA_PENDING_ONLY = "pendingOnly";

    private AccountDeletionViewModel viewModel;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmationInput;
    private TextInputEditText reasonInput;
    private TextInputLayout passwordLayout;
    private MaterialButton requestButton;
    private TextView statusText;
    private View formContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_deletion);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAccountDeletion);
        toolbar.setNavigationOnClickListener(view -> finish());
        passwordInput = findViewById(R.id.editDeletionPassword);
        confirmationInput = findViewById(R.id.editDeletionConfirmation);
        reasonInput = findViewById(R.id.editDeletionReason);
        passwordLayout = findViewById(R.id.layoutDeletionPassword);
        requestButton = findViewById(R.id.buttonRequestDeletion);
        statusText = findViewById(R.id.textDeletionStatus);
        formContainer = findViewById(R.id.containerDeletionForm);

        viewModel = new ViewModelProvider(this).get(AccountDeletionViewModel.class);
        viewModel.getState().observe(this, this::renderState);

        if (!viewModel.usesPasswordProvider()) {
            passwordLayout.setVisibility(View.GONE);
        }

        requestButton.setOnClickListener(view -> viewModel.requestDeletion(
                textOf(passwordInput),
                textOf(confirmationInput),
                textOf(reasonInput)));

        findViewById(R.id.buttonDeletionSignOut).setOnClickListener(view -> signOut());

        if (getIntent().getBooleanExtra(EXTRA_PENDING_ONLY, false)) {
            showPendingState();
        } else {
            viewModel.checkPending();
        }
    }

    private void renderState(AccountDeletionViewModel.State state) {
        requestButton.setEnabled(!state.loading);
        passwordInput.setEnabled(!state.loading);
        confirmationInput.setEnabled(!state.loading);
        reasonInput.setEnabled(!state.loading);
        requestButton.setText(state.loading
                ? R.string.account_delete_request_loading
                : R.string.account_delete_request_button);
        if (state.pending || state.success) {
            showPendingState();
        } else if (state.message != null && !state.message.isEmpty()) {
            showStatus(state.message, true);
        }
    }

    private void showPendingState() {
        formContainer.setVisibility(View.GONE);
        showStatus(getString(R.string.account_delete_pending_message), false);
    }

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(getColor(error
                ? R.color.quote_status_error
                : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
