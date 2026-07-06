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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
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
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
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
