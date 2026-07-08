package com.merg.quoteapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.merg.quoteapp.ui.discover.DiscoverFragment;
import com.merg.quoteapp.ui.favorites.FavoritesFragment;
import com.merg.quoteapp.ui.home.HomeFragment;
import com.merg.quoteapp.ui.profile.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_PROFILE_TAB = "openProfileTab";

    private BottomNavigationView bottomNavigation;

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
}
