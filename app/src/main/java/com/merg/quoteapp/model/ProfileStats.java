package com.merg.quoteapp.model;

public class ProfileStats {

    private final String username;
    private final String email;
    private final int totalQuotes;
    private final int movieQuotes;
    private final int seriesQuotes;
    private final int bookQuotes;

    public ProfileStats(String username, String email, int totalQuotes,
                        int movieQuotes, int seriesQuotes, int bookQuotes) {
        this.username = username;
        this.email = email;
        this.totalQuotes = totalQuotes;
        this.movieQuotes = movieQuotes;
        this.seriesQuotes = seriesQuotes;
        this.bookQuotes = bookQuotes;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
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
}
