package com.merg.quoteapp.utils;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.repository.AccountDeletionRepository;
import com.merg.quoteapp.ui.profile.AccountDeletionActivity;

public final class AccountDeletionGuard {

    private AccountDeletionGuard() {
    }

    public static void enforce(Activity activity) {
        if (activity == null || activity.isFinishing()
                || activity instanceof AccountDeletionActivity
                || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        AccountDeletionRepository.getInstance().checkCurrentUserDeletionState(state -> {
            if (activity.isFinishing()) {
                return;
            }
            if (state == AccountDeletionRepository.DeletionState.PENDING) {
                openPendingAndClearTask(activity, false);
            } else if (state == AccountDeletionRepository.DeletionState.UNKNOWN) {
                openPendingAndClearTask(activity, true);
            }
        });
    }

    public static void openPendingAndClearTask(Activity activity, boolean checkFailed) {
        Intent intent = new Intent(activity, AccountDeletionActivity.class);
        intent.putExtra(AccountDeletionActivity.EXTRA_PENDING_ONLY, true);
        intent.putExtra(AccountDeletionActivity.EXTRA_CHECK_FAILED, checkFailed);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void openMainAndClearTask(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
