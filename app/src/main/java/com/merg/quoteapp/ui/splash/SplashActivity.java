package com.merg.quoteapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.repository.AccountDeletionRepository;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.ui.profile.AccountDeletionActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MILLIS = 1200L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable openNextScreen = () -> {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openScreen(LoginActivity.class, false);
            return;
        }
        AccountDeletionRepository.getInstance().checkCurrentUserDeletionState(state -> {
            boolean locked = state == AccountDeletionRepository.DeletionState.PENDING
                    || state == AccountDeletionRepository.DeletionState.UNKNOWN;
            openScreen(locked ? AccountDeletionActivity.class : MainActivity.class,
                    locked,
                    state == AccountDeletionRepository.DeletionState.UNKNOWN);
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(openNextScreen, SPLASH_DELAY_MILLIS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openNextScreen);
        super.onDestroy();
    }

    private void openScreen(Class<?> destination, boolean pendingOnly) {
        openScreen(destination, pendingOnly, false);
    }

    private void openScreen(Class<?> destination, boolean pendingOnly, boolean checkFailed) {
        Intent intent = new Intent(this, destination);
        if (pendingOnly) {
            intent.putExtra(AccountDeletionActivity.EXTRA_PENDING_ONLY, true);
            intent.putExtra(AccountDeletionActivity.EXTRA_CHECK_FAILED, checkFailed);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
