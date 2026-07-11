package com.merg.quoteapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PasswordResetCooldownManager {

    private static final String PREFS_NAME = "password_reset_cooldown";
    private static final String KEY_UNTIL = "cooldown_until";
    private static final long COOLDOWN_MILLIS = 150_000L;

    private final SharedPreferences preferences;

    public PasswordResetCooldownManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void startCooldown() {
        preferences.edit()
                .putLong(KEY_UNTIL, System.currentTimeMillis() + COOLDOWN_MILLIS)
                .apply();
    }

    public long remainingSeconds() {
        long remainingMillis = preferences.getLong(KEY_UNTIL, 0L) - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return 0;
        }
        return Math.max(1, (remainingMillis + 999L) / 1000L);
    }

    public boolean isCoolingDown() {
        return remainingSeconds() > 0;
    }
}
