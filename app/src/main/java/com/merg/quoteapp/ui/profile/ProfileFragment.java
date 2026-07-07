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

import com.google.firebase.auth.FirebaseAuth;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.ProfileStats;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.ui.auth.LoginActivity;
import com.merg.quoteapp.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private TextView usernameText;
    private TextView emailText;
    private TextView statusText;
    private ProgressBar progressBar;

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
        setupStatLabels(view);

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getProfile().observe(getViewLifecycleOwner(), stats ->
                renderProfile(view, stats));
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.loadProfile();

        view.findViewById(R.id.buttonLogout).setOnClickListener(button -> logout());
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

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
