package com.merg.quoteapp.model;

public class UserProfilePage {

    private final UserProfileData profile;
    private final boolean hasMore;

    public UserProfilePage(UserProfileData profile, boolean hasMore) {
        this.profile = profile;
        this.hasMore = hasMore;
    }

    public UserProfileData getProfile() {
        return profile;
    }

    public boolean hasMore() {
        return hasMore;
    }
}
