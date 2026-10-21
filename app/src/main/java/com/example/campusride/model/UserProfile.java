package com.example.campusride.model;

public class UserProfile {
    private String userId;
    private String displayName;
    private String email;
    private String photoUrl;
    private String academicInfo;
    private int ridesShared;
    private double peerRating;
    private double co2SavedKg;
    private String fcmToken;

    public UserProfile() {
    }

    public UserProfile(String userId, String displayName, String email, String photoUrl,
                       String academicInfo, int ridesShared, double peerRating, double co2SavedKg) {
        this(userId, displayName, email, photoUrl, academicInfo, ridesShared, peerRating, co2SavedKg, "");
    }

    public UserProfile(String userId, String displayName, String email, String photoUrl,
                       String academicInfo, int ridesShared, double peerRating, double co2SavedKg,
                       String fcmToken) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.academicInfo = academicInfo;
        this.ridesShared = ridesShared;
        this.peerRating = peerRating;
        this.co2SavedKg = co2SavedKg;
        this.fcmToken = fcmToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getAcademicInfo() {
        return academicInfo;
    }

    public void setAcademicInfo(String academicInfo) {
        this.academicInfo = academicInfo;
    }

    public int getRidesShared() {
        return ridesShared;
    }

    public void setRidesShared(int ridesShared) {
        this.ridesShared = ridesShared;
    }

    public double getPeerRating() {
        return peerRating;
    }

    public void setPeerRating(double peerRating) {
        this.peerRating = peerRating;
    }

    public double getCo2SavedKg() {
        return co2SavedKg;
    }

    public void setCo2SavedKg(double co2SavedKg) {
        this.co2SavedKg = co2SavedKg;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}