package com.example.campusride.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central zone configuration model for CampusRide.
 * Defines stable zone IDs (Z1-Z7, T1-T4) and exact geographical coordinates.
 */
public class CampusZoneConfig {

    public static class ZoneInfo {
        private final String id;
        private final String name;
        private final double latitude;
        private final double longitude;
        private final boolean isPickup;

        public ZoneInfo(String id, String name, double latitude, double longitude, boolean isPickup) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.isPickup = isPickup;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public boolean isPickup() { return isPickup; }

        public String getFormattedLabel() {
            return id + " - " + name;
        }
    }

    private static final List<ZoneInfo> PICKUP_ZONES = new ArrayList<>();
    private static final List<ZoneInfo> TRANSIT_HUBS = new ArrayList<>();

    static {
        // Exact 7 Pickup Zones (Z1 - Z7)
        PICKUP_ZONES.add(new ZoneInfo("Z1", "NMIMS Main Gate", 19.103336, 72.836780, true));
        PICKUP_ZONES.add(new ZoneInfo("Z2", "Mithibai Gate 1", 19.103331, 72.836980, true));
        PICKUP_ZONES.add(new ZoneInfo("Z3", "Mithibai Gate 2", 19.102590, 72.837281, true));
        PICKUP_ZONES.add(new ZoneInfo("Z4", "DJ Sanghvi Gate", 19.107206, 72.836975, true));
        PICKUP_ZONES.add(new ZoneInfo("Z5", "Opp DJ Sanghvi", 19.107233, 72.836727, true));
        PICKUP_ZONES.add(new ZoneInfo("Z6", "NMIMS Mukesh Patel", 19.108056, 72.836999, true));
        PICKUP_ZONES.add(new ZoneInfo("Z7", "Old Mukesh Gate", 19.109682, 72.837240, true));

        // Exact 4 Revised Transit Hubs (T1 - T4)
        TRANSIT_HUBS.add(new ZoneInfo("T1", "DN Nagar Metro Station (Yellow and Blue Line)", 19.127970, 72.830969, false));
        TRANSIT_HUBS.add(new ZoneInfo("T2", "Gundavli Metro Station (Red Line)", 19.113311, 72.854992, false));
        TRANSIT_HUBS.add(new ZoneInfo("T3", "Vile Parle Station", 19.100122, 72.843751, false));
        TRANSIT_HUBS.add(new ZoneInfo("T4", "Andheri Station", 19.119651, 72.846287, false));
    }

    public static List<ZoneInfo> getPickupZones() {
        return Collections.unmodifiableList(PICKUP_ZONES);
    }

    public static List<ZoneInfo> getTransitHubs() {
        return Collections.unmodifiableList(TRANSIT_HUBS);
    }

    public static ZoneInfo findZoneByIdOrName(String query) {
        if (query == null) return null;
        String q = query.trim();

        // Support backward compatibility with H1-H4 IDs or T1-T4 IDs
        if (q.equalsIgnoreCase("H1") || q.equalsIgnoreCase("Transit Hub 1")) return TRANSIT_HUBS.get(0);
        if (q.equalsIgnoreCase("H2") || q.equalsIgnoreCase("Transit Hub 2")) return TRANSIT_HUBS.get(1);
        if (q.equalsIgnoreCase("H3") || q.equalsIgnoreCase("Transit Hub 3")) return TRANSIT_HUBS.get(2);
        if (q.equalsIgnoreCase("H4") || q.equalsIgnoreCase("Transit Hub 4")) return TRANSIT_HUBS.get(3);

        for (ZoneInfo z : PICKUP_ZONES) {
            if (z.getId().equalsIgnoreCase(q) || z.getName().equalsIgnoreCase(q)
                    || z.getFormattedLabel().equalsIgnoreCase(q)
                    || q.contains(z.getName()) || z.getName().contains(q)) {
                return z;
            }
        }
        for (ZoneInfo z : TRANSIT_HUBS) {
            if (z.getId().equalsIgnoreCase(q) || z.getName().equalsIgnoreCase(q)
                    || z.getFormattedLabel().equalsIgnoreCase(q)
                    || q.contains(z.getName()) || z.getName().contains(q)) {
                return z;
            }
        }
        return null;
    }
}