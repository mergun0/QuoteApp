package com.merg.quoteapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Like {

    private String likeId;
    private String quoteId;
    private String userId;
    private Timestamp createdAt;

    /**
     * Creates an empty Like instance for Firebase and Java object mapping.
     */
    public Like() {
    }

    /**
     * Creates a Like instance with the fields used by the app layer.
     *
     * @param likeId unique document id for the like
     * @param quoteId id of the liked quote
     * @param userId id of the user who liked the quote
     * @param createdAt creation timestamp of the like
     */
    public Like(String likeId, String quoteId, String userId, Timestamp createdAt) {
        this.likeId = likeId;
        this.quoteId = quoteId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    /**
     * Returns the document id for this like.
     *
     * @return like document id
     */
    @Exclude
    public String getLikeId() {
        return likeId;
    }

    /**
     * Sets the document id for this like.
     *
     * @param likeId like document id
     */
    @Exclude
    public void setLikeId(String likeId) {
        this.likeId = likeId;
    }

    /**
     * Returns the id of the liked quote.
     *
     * @return quote id
     */
    public String getQuoteId() {
        return quoteId;
    }

    /**
     * Sets the id of the liked quote.
     *
     * @param quoteId quote id
     */
    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    /**
     * Returns the id of the user who liked the quote.
     *
     * @return user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the id of the user who liked the quote.
     *
     * @param userId user id
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return creation timestamp
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
