package com.example.campusride.model;

public class GateDemand {
    private String gateId;
    private String gateName;
    private double latitude;
    private double longitude;
    private int estimatedOutflow;
    private int timeUntilDismissal;
    private Double driverAvailability;
    private double priorityScore;
    private String priorityLabel; // LOW, MEDIUM, HIGH, CRITICAL
    private String explanation;

    public GateDemand() {
    }

    public GateDemand(String gateId, String gateName, double latitude, double longitude,
                      int estimatedOutflow, int timeUntilDismissal, Double driverAvailability,
                      double priorityScore, String priorityLabel, String explanation) {
        this.gateId = gateId;
        this.gateName = gateName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedOutflow = estimatedOutflow;
        this.timeUntilDismissal = timeUntilDismissal;
        this.driverAvailability = driverAvailability;
        this.priorityScore = priorityScore;
        this.priorityLabel = priorityLabel;
        this.explanation = explanation;
    }

    public String getGateId() {
        return gateId;
    }

    public void setGateId(String gateId) {
        this.gateId = gateId;
    }

    public String getGateName() {
        return gateName;
    }

    public void setGateName(String gateName) {
        this.gateName = gateName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getEstimatedOutflow() {
        return estimatedOutflow;
    }

    public void setEstimatedOutflow(int estimatedOutflow) {
        this.estimatedOutflow = estimatedOutflow;
    }

    public int getTimeUntilDismissal() {
        return timeUntilDismissal;
    }

    public void setTimeUntilDismissal(int timeUntilDismissal) {
        this.timeUntilDismissal = timeUntilDismissal;
    }

    public Double getDriverAvailability() {
        return driverAvailability;
    }

    public void setDriverAvailability(Double driverAvailability) {
        this.driverAvailability = driverAvailability;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}