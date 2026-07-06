package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;

public class User {

    private String uid;
    private String username;
    private String email;
    private Timestamp createdAt;

    public User() {
        // Required by Firestore.
    }

    public User(String uid, String username, String email, Timestamp createdAt) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
