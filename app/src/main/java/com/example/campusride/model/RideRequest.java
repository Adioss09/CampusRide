package com.example.campusride.model;

public class RideRequest {
    private String requestId;
    private String userId;
    private String userName;
    private String pickupGate;
    private String destinationHub;
    private String departureSlot;
    private boolean willingToShare;
    private String status; // PENDING, MATCHED, COMPLETED, CANCELLED
    private long timestamp;

    public RideRequest() {
    }

    public RideRequest(String requestId, String userId, String userName, String pickupGate,
                       String destinationHub, String departureSlot, boolean willingToShare,
                       String status, long timestamp) {
        this.requestId = requestId;
        this.userId = userId;
        this.userName = userName;
        this.pickupGate = pickupGate;
        this.destinationHub = destinationHub;
        this.departureSlot = departureSlot;
        this.willingToShare = willingToShare;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPickupGate() {
        return pickupGate;
    }

    public void setPickupGate(String pickupGate) {
        this.pickupGate = pickupGate;
    }

    public String getDestinationHub() {
        return destinationHub;
    }

    public void setDestinationHub(String destinationHub) {
        this.destinationHub = destinationHub;
    }

    public String getDepartureSlot() {
        return departureSlot;
    }

    public void setDepartureSlot(String departureSlot) {
        this.departureSlot = departureSlot;
    }

    public boolean isWillingToShare() {
        return willingToShare;
    }

    public void setWillingToShare(boolean willingToShare) {
        this.willingToShare = willingToShare;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}