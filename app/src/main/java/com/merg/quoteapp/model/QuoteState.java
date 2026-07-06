package com.merg.quoteapp.model;

public class QuoteState {

    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final String message;

    private QuoteState(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static QuoteState loading() {
        return new QuoteState(Status.LOADING, null);
    }

    public static QuoteState success(String message) {
        return new QuoteState(Status.SUCCESS, message);
    }

    public static QuoteState error(String message) {
        return new QuoteState(Status.ERROR, message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
