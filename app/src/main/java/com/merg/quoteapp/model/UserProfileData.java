package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;

import java.util.List;

public class UserProfileData {

    private final String userId;
    private final String username;
    private final Timestamp joinedAt;
    private final int totalQuotes;
    private final int movieQuotes;
    private final int seriesQuotes;
    private final int bookQuotes;
    private final int totalLikes;
    private final List<Quote> quotes;

    public UserProfileData(String userId, String username, Timestamp joinedAt,
                           int totalQuotes, int movieQuotes, int seriesQuotes,
                           int bookQuotes, int totalLikes, List<Quote> quotes) {
        this.userId = userId;
        this.username = username;
        this.joinedAt = joinedAt;
        this.totalQuotes = totalQuotes;
        this.movieQuotes = movieQuotes;
        this.seriesQuotes = seriesQuotes;
        this.bookQuotes = bookQuotes;
        this.totalLikes = totalLikes;
        this.quotes = quotes;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public int getTotalQuotes() {
        return totalQuotes;
    }

    public int getMovieQuotes() {
        return movieQuotes;
    }

    public int getSeriesQuotes() {
        return seriesQuotes;
    }

    public int getBookQuotes() {
        return bookQuotes;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public List<Quote> getQuotes() {
        return quotes;
    }
}
