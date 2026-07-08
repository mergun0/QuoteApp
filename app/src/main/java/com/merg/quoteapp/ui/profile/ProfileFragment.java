package com.merg.quoteapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.merg.quoteapp.R;
import com.merg.quoteapp.adapter.AchievementPreviewAdapter;
import com.merg.quoteapp.model.Achievement;
import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.model.UserAchievement;
import com.merg.quoteapp.model.UserStats;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.viewmodel.AchievementViewModel;
import com.merg.quoteapp.viewmodel.LevelViewModel;
import com.merg.quoteapp.viewmodel.ProfileViewModel;
import com.merg.quoteapp.viewmodel.UserStatsViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView usernameText;
    private TextView emailText;
    private TextView statusText;
    private TextView levelText;
    private TextView xpText;
    private TextView xpProgressText;
    private TextView achievementCountText;
    private TextView achievementEmptyText;
    private ProgressBar progressBar;
    private ProgressBar xpProgressBar;
    private RecyclerView achievementRecyclerView;
    private AchievementPreviewAdapter achievementAdapter;
    private UserStatsViewModel userStatsViewModel;
    private AchievementViewModel achievementViewModel;
    private LevelViewModel levelViewModel;
    private UserStats currentStats;
    private List<Achievement> currentAchievements = new ArrayList<>();
    private List<UserAchievement> currentUserAchievements = new ArrayList<>();

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usernameText = view.findViewById(R.id.textProfileUsername);
        emailText = view.findViewById(R.id.textProfileEmail);
        statusText = view.findViewById(R.id.textProfileStatus);
        progressBar = view.findViewById(R.id.progressProfile);
        levelText = view.findViewById(R.id.textProfileLevel);
        xpText = view.findViewById(R.id.textProfileXp);
        xpProgressText = view.findViewById(R.id.textProfileXpProgress);
        achievementCountText = view.findViewById(R.id.textProfileAchievementCount);
        achievementEmptyText = view.findViewById(R.id.textProfileAchievementEmpty);
        xpProgressBar = view.findViewById(R.id.progressProfileXp);
        achievementRecyclerView = view.findViewById(R.id.recyclerProfileAchievements);
        userStatsViewModel = new ViewModelProvider(this).get(UserStatsViewModel.class);
        achievementViewModel = new ViewModelProvider(this).get(AchievementViewModel.class);
        levelViewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        setupStatLabels(view);
        setupAchievementPreview();

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getProfile().observe(getViewLifecycleOwner(), stats ->
                renderProfile(view, stats));
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.loadProfile();

        observeAchievementState();
        loadAchievementState();

        view.findViewById(R.id.buttonLogout).setOnClickListener(button -> logout());
        view.findViewById(R.id.buttonProfileAllAchievements)
                .setOnClickListener(button -> openAchievements());
    }

    private void setupStatLabels(View view) {
        setStatLabel(view.findViewById(R.id.statTotal), R.string.profile_total);
        setStatLabel(view.findViewById(R.id.statMovies), R.string.profile_movies);
        setStatLabel(view.findViewById(R.id.statSeries), R.string.profile_series);
        setStatLabel(view.findViewById(R.id.statBooks), R.string.profile_books);
        setStatLabel(view.findViewById(R.id.statLikes), R.string.profile_likes);
    }

    private void setStatLabel(View statView, int labelRes) {
        ((TextView) statView.findViewById(R.id.textStatLabel)).setText(labelRes);
    }

    private void renderProfile(View root, ProfileStats stats) {
        usernameText.setText(stats.getUsername());
        emailText.setText(stats.getEmail());
        setStatValue(root.findViewById(R.id.statTotal), stats.getTotalQuotes());
        setStatValue(root.findViewById(R.id.statMovies), stats.getMovieQuotes());
        setStatValue(root.findViewById(R.id.statSeries), stats.getSeriesQuotes());
        setStatValue(root.findViewById(R.id.statBooks), stats.getBookQuotes());
        setStatValue(root.findViewById(R.id.statLikes), stats.getTotalLikes());
    }

    private void setStatValue(View statView, int value) {
        ((TextView) statView.findViewById(R.id.textStatValue))
                .setText(String.valueOf(value));
    }

    private void renderState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (state.getStatus() == QuoteState.Status.ERROR) {
            statusText.setText(state.getMessage());
            statusText.setVisibility(View.VISIBLE);
        } else {
            statusText.setVisibility(View.GONE);
        }
    }

    private void setupAchievementPreview() {
        achievementAdapter = new AchievementPreviewAdapter();
        achievementRecyclerView.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        achievementRecyclerView.setAdapter(achievementAdapter);
        renderDefaultAchievementState();
    }

    private void observeAchievementState() {
        if (userStatsViewModel == null || achievementViewModel == null) {
            return;
        }
        userStatsViewModel.getUserStats().observe(getViewLifecycleOwner(), stats -> {
            currentStats = stats == null ? defaultStats() : stats;
            renderUserStats();
            if (levelViewModel != null) {
                levelViewModel.loadLevelProgress(currentStats.getTotalXp());
            }
        });
        userStatsViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                renderDefaultAchievementState();
            }
        });
        if (levelViewModel != null) {
            levelViewModel.getCurrentLevel().observe(getViewLifecycleOwner(), level -> renderUserStats());
            levelViewModel.getNextLevel().observe(getViewLifecycleOwner(), level -> renderUserStats());
        }
        achievementViewModel.getActiveAchievements().observe(getViewLifecycleOwner(), achievements -> {
            currentAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
        });
        achievementViewModel.getUserAchievements().observe(getViewLifecycleOwner(), achievements -> {
            currentUserAchievements = achievements == null ? new ArrayList<>() : achievements;
            renderAchievementPreview();
            renderUserStats();
        });
        achievementViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && currentAchievements.isEmpty()) {
                renderAchievementPreview();
            }
        });
    }

    private void loadAchievementState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            renderDefaultAchievementState();
            return;
        }
        if (userStatsViewModel == null || achievementViewModel == null) {
            renderDefaultAchievementState();
            return;
        }
        userStatsViewModel.createDefaultUserStatsIfMissing(user.getUid());
        achievementViewModel.loadActiveAchievements();
        achievementViewModel.loadUserAchievements(user.getUid());
    }

    private void renderDefaultAchievementState() {
        currentStats = defaultStats();
        renderUserStats();
        renderAchievementPreview();
    }

    private void renderUserStats() {
        if (levelText == null || xpText == null || achievementCountText == null
                || xpProgressBar == null || xpProgressText == null) {
            return;
        }
        UserStats stats = currentStats == null ? defaultStats() : currentStats;
        int level = stats.getLevel() <= 0 ? 1 : stats.getLevel();
        long totalXp = Math.max(0, stats.getTotalXp());
        long unlockedCount = Math.max(stats.getUnlockedAchievementCount(),
                currentUserAchievements == null ? 0 : currentUserAchievements.size());

        levelText.setText(getString(R.string.level_format, level));
        xpText.setText(getString(R.string.xp_total_format, totalXp));
        achievementCountText.setText(getString(
                R.string.unlocked_achievement_count_format, unlockedCount));

        if (levelViewModel == null || levelViewModel.getCurrentLevel() == null
                || levelViewModel.getNextLevel() == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }

        long currentRequiredXp = levelViewModel.getCurrentLevel().getValue() == null
                ? 0 : levelViewModel.getCurrentLevel().getValue().getRequiredTotalXp();
        if (levelViewModel.getNextLevel().getValue() == null) {
            renderFallbackXpProgress(totalXp);
            return;
        }
        long nextRequiredXp = levelViewModel.getNextLevel().getValue().getRequiredTotalXp();
        long range = Math.max(1, nextRequiredXp - currentRequiredXp);
        long progress = Math.max(0, totalXp - currentRequiredXp);
        xpProgressBar.setProgress((int) Math.min(100, (progress * 100) / range));
        xpProgressText.setText(getString(R.string.xp_progress_format, progress, range));
    }

    private void renderFallbackXpProgress(long totalXp) {
        long fallbackTarget = 100;
        long safeXp = Math.max(0, totalXp);
        xpProgressBar.setProgress((int) Math.min(100, (safeXp * 100) / fallbackTarget));
        xpProgressText.setText(getString(R.string.xp_progress_format,
                Math.min(safeXp, fallbackTarget), fallbackTarget));
    }

    private void renderAchievementPreview() {
        if (achievementAdapter == null || achievementRecyclerView == null
                || achievementEmptyText == null) {
            return;
        }
        achievementAdapter.submitData(currentAchievements, currentUserAchievements);
        boolean empty = currentAchievements == null || currentAchievements.isEmpty();
        achievementRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        achievementEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private UserStats defaultStats() {
        UserStats stats = new UserStats();
        stats.setLevel(1);
        stats.setTotalXp(0);
        stats.setUnlockedAchievementCount(0);
        return stats;
    }

    private void openAchievements() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), AchievementsActivity.class);
        intent.putExtra(AchievementsActivity.EXTRA_USER_ID, user.getUid());
        startActivity(intent);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
