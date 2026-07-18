package com.merg.quoteapp;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.merg.quoteapp.ui.discover.DiscoverFragment;
import com.merg.quoteapp.ui.favorites.FavoritesFragment;
import com.merg.quoteapp.ui.home.HomeFragment;
import com.merg.quoteapp.ui.profile.ProfileFragment;
import com.merg.quoteapp.model.AchievementFeedbackEvent;
import com.merg.quoteapp.utils.AccountDeletionGuard;
import com.merg.quoteapp.utils.AchievementFeedbackCenter;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_PROFILE_TAB = "openProfileTab";

    private BottomNavigationView bottomNavigation;
    private boolean feedbackShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_discover) {
                fragment = new DiscoverFragment();
            } else if (itemId == R.id.navigation_favorites) {
                fragment = new FavoritesFragment();
            } else if (itemId == R.id.navigation_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeFragment();
            }   
            showFragment(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(EXTRA_OPEN_PROFILE_TAB, false)) {
                openProfileTab();
            } else {
                bottomNavigation.setSelectedItemId(R.id.navigation_home);
            }
        }
        observeAchievementFeedback();
        AccountDeletionGuard.enforce(this);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_PROFILE_TAB, false)) {
            openProfileTab();
        }
    }

    public void openProfileTab() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_profile);
        }
    }

    public void openFavoritesTab() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_favorites);
        }
    }

    private void observeAchievementFeedback() {
        AchievementFeedbackCenter.getInstance()
                .getEventSignal()
                .observe(this, signal -> showNextFeedbackEvent());
    }

    private void showNextFeedbackEvent() {
        if (feedbackShowing) {
            return;
        }
        AchievementFeedbackEvent event = AchievementFeedbackCenter.getInstance().poll();
        if (event == null) {
            return;
        }
        feedbackShowing = true;
        if (event.getType() == AchievementFeedbackEvent.Type.XP_GAINED) {
            showXpFeedback(event);
        } else if (event.getType() == AchievementFeedbackEvent.Type.ACHIEVEMENT_UNLOCKED) {
            showAchievementUnlockedFeedback(event);
        } else if (event.getType() == AchievementFeedbackEvent.Type.LEVEL_UP) {
            showLevelUpFeedback(event);
        }
    }

    private void finishFeedbackEvent() {
        feedbackShowing = false;
        if (AchievementFeedbackCenter.getInstance().hasPendingEvents()) {
            showNextFeedbackEvent();
        }
    }

    private void showXpFeedback(AchievementFeedbackEvent event) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main),
                        getString(R.string.xp_gained_format, event.getXpAmount()),
                        Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getColor(R.color.home_v2_card));
        snackbar.setTextColor(getColor(R.color.home_v2_text_primary))
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        finishFeedbackEvent();
                    }
                });
        snackbar.show();
    }

    private void showAchievementUnlockedFeedback(AchievementFeedbackEvent event) {
        String message = getString(R.string.achievement_feedback_message,
                safe(event.getTitle()),
                safe(event.getDescription()),
                event.getXpAmount());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.achievement_unlocked_feedback_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(dialogInterface -> finishFeedbackEvent())
                .show();
        styleFeedbackDialog(dialog);
    }

    private void showLevelUpFeedback(AchievementFeedbackEvent event) {
        String detail = safe(event.getLevelTitle());
        String badge = safe(event.getBadgeName());
        if (!badge.isEmpty() && !badge.equalsIgnoreCase(detail)) {
            detail = detail.isEmpty() ? badge : detail + " • " + badge;
        }
        String message = detail.isEmpty()
                ? getString(R.string.level_up_feedback_message_simple, event.getLevel())
                : getString(R.string.level_up_feedback_message, event.getLevel(), detail);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.level_up_feedback_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(dialogInterface -> finishFeedbackEvent())
                .show();
        styleFeedbackDialog(dialog);
    }

    private void styleFeedbackDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(getColor(R.color.home_v2_card)));
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getColor(R.color.home_v2_primary));
    }
    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.fragment_fade_in,
                        R.anim.fragment_fade_out,
                        R.anim.fragment_fade_in,
                        R.anim.fragment_fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

}
