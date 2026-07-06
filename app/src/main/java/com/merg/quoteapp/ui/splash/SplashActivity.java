package com.merg.quoteapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.MainActivity;
import com.merg.quoteapp.R;
import com.merg.quoteapp.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MILLIS = 1200L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable openNextScreen = () -> {
        boolean signedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        Class<?> destination = signedIn ? MainActivity.class : LoginActivity.class;

        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
}
