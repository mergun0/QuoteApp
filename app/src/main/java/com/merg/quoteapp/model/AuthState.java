package com.merg.quoteapp.model;

public class AuthState {

    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final String message;

    private AuthState(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static AuthState loading() {
        return new AuthState(Status.LOADING, null);
    }

    public static AuthState success(String message) {
        return new AuthState(Status.SUCCESS, message);
    }

    public static AuthState error(String message) {
        return new AuthState(Status.ERROR, message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
