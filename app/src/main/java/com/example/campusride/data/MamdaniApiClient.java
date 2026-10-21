package com.example.campusride.data;

import com.example.campusride.model.GateDemand;

import java.util.Locale;

/**
 * Client interface & engine evaluating Mamdani Fuzzy Inference System scores for CampusRide.
 */
public class MamdaniApiClient {

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private static MamdaniApiClient instance;

    private MamdaniApiClient() {
    }

    public static synchronized MamdaniApiClient getInstance() {
        if (instance == null) {
            instance = new MamdaniApiClient();
        }
        return instance;
    }

    public GateDemand evaluateZonePriority(String zoneId, String zoneName, double lat, double lng,
                                           double outflow, double timeUntilDismissal, Double driverAvailability) {
        double score;
        String label;

        if (outflow >= 180 && timeUntilDismissal <= 5) {
            score = 0.92;
            label = "CRITICAL";
        } else if (outflow >= 120 && timeUntilDismissal <= 10) {
            score = 0.78;
            label = "HIGH";
        } else if (outflow >= 60 || timeUntilDismissal <= 20) {
            score = 0.50;
            label = "MEDIUM";
        } else {
            score = 0.20;
            label = "LOW";
        }

        String explanation = "Demand score = " + String.format(Locale.US, "%.2f", score)
                + " (" + label + ") based on ~" + (int) outflow + " estimated outflow & "
                + (int) timeUntilDismissal + " min dismissal window.";

        return new GateDemand(
                zoneId,
                zoneName,
                lat,
                lng,
                (int) outflow,
                (int) timeUntilDismissal,
                driverAvailability,
                score,
                label,
                explanation
        );
    }

    public void evaluatePriority(double outflow, double timeUntilDismissal, Double driverAvailability, ApiCallback<GateDemand> callback) {
        GateDemand result = evaluateZonePriority("Z1", "Evaluated Zone", 19.103336, 72.836780, outflow, timeUntilDismissal, driverAvailability);
        if (callback != null) {
            callback.onSuccess(result);
        }
    }
}