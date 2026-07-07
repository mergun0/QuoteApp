package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;

public class User {

    private String uid;
    private String username;
    private String email;
    private Timestamp createdAt;
    private int validReports;
    private int invalidReports;
    private Timestamp reportRestrictionUntil;

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

    public int getValidReports() {
        return validReports;
    }

    public void setValidReports(int validReports) {
        this.validReports = validReports;
    }

    public int getInvalidReports() {
        return invalidReports;
    }

    public void setInvalidReports(int invalidReports) {
        this.invalidReports = invalidReports;
    }

    public Timestamp getReportRestrictionUntil() {
        return reportRestrictionUntil;
    }

    public void setReportRestrictionUntil(Timestamp reportRestrictionUntil) {
        this.reportRestrictionUntil = reportRestrictionUntil;
    }
}
