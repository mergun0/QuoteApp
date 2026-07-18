package com.merg.quoteapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.R;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.utils.AccountDeletionGuard;
import com.merg.quoteapp.viewmodel.AccountDeletionViewModel;

public class AccountDeletionActivity extends AppCompatActivity {

    public static final String EXTRA_PENDING_ONLY = "pendingOnly";
    public static final String EXTRA_CHECK_FAILED = "checkFailed";

    private AccountDeletionViewModel viewModel;
    private MaterialToolbar toolbar;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmationInput;
    private TextInputEditText reasonInput;
    private TextInputLayout passwordLayout;
    private MaterialButton requestButton;
    private MaterialButton retryButton;
    private TextView statusText;
    private View formContainer;
    private boolean pendingMode;
    private boolean checkFailedMode;
    private boolean successRelaunchStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_deletion);

        toolbar = findViewById(R.id.toolbarAccountDeletion);
        toolbar.setNavigationOnClickListener(view -> finish());
        passwordInput = findViewById(R.id.editDeletionPassword);
        confirmationInput = findViewById(R.id.editDeletionConfirmation);
        reasonInput = findViewById(R.id.editDeletionReason);
        passwordLayout = findViewById(R.id.layoutDeletionPassword);
        requestButton = findViewById(R.id.buttonRequestDeletion);
        retryButton = findViewById(R.id.buttonDeletionRetry);
        statusText = findViewById(R.id.textDeletionStatus);
        formContainer = findViewById(R.id.containerDeletionForm);
        pendingMode = getIntent().getBooleanExtra(EXTRA_PENDING_ONLY, false);
        checkFailedMode = getIntent().getBooleanExtra(EXTRA_CHECK_FAILED, false);

        viewModel = new ViewModelProvider(this).get(AccountDeletionViewModel.class);
        viewModel.getState().observe(this, this::renderState);
        setupBackHandling();

        if (!viewModel.usesPasswordProvider()) {
            passwordLayout.setVisibility(View.GONE);
        }

        requestButton.setOnClickListener(view -> viewModel.requestDeletion(
                textOf(passwordInput),
                textOf(confirmationInput),
                textOf(reasonInput)));

        findViewById(R.id.buttonDeletionSignOut).setOnClickListener(view -> signOut());
        retryButton.setOnClickListener(view -> {
            retryButton.setVisibility(View.GONE);
            viewModel.checkPending();
        });

        if (checkFailedMode) {
            showCheckFailedState();
        } else if (pendingMode) {
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
        if (state.loading) {
            showStatus(getString(R.string.operation_in_progress), false);
        } else if (state.unknown) {
            showCheckFailedState();
        } else if (state.success) {
            openPendingScreenAfterSuccess();
        } else if (state.pending) {
            showPendingState();
        } else if (checkFailedMode && !state.pending) {
            AccountDeletionGuard.openMainAndClearTask(this);
        } else if (state.message != null && !state.message.isEmpty()) {
            showStatus(state.message, true);
        } else {
            hideStatus();
        }
    }

    private void showPendingState() {
        pendingMode = true;
        checkFailedMode = false;
        disableBackNavigation();
        formContainer.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        showStatus(getString(R.string.account_delete_pending_message), false);
    }

    private void showCheckFailedState() {
        checkFailedMode = true;
        pendingMode = true;
        disableBackNavigation();
        formContainer.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        showStatus(getString(R.string.account_delete_check_failed), true);
    }

    private void openPendingScreenAfterSuccess() {
        if (successRelaunchStarted) {
            return;
        }
        successRelaunchStarted = true;
        AccountDeletionGuard.openPendingAndClearTask(this, false);
    }

    private void disableBackNavigation() {
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!pendingMode && !checkFailedMode) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(getColor(error
                ? R.color.quote_status_error
                : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusText.setText("");
        statusText.setVisibility(View.GONE);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
