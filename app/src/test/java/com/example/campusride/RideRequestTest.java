package com.example.campusride;

import com.example.campusride.model.RideRequest;
import com.example.campusride.model.UserProfile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RideRequestTest {

    @Test
    public void testRideRequestCreationAndGetters() {
        RideRequest request = new RideRequest(
                "doc_123",
                "user_abc",
                "Alex Student",
                "Z3 - Mithibai Gate 2",
                "T1 - DN Nagar Metro Station",
                "4:50 PM",
                true,
                "PENDING",
                1700000000000L
        );

        assertEquals("doc_123", request.getRequestId());
        assertEquals("user_abc", request.getUserId());
        assertEquals("Alex Student", request.getUserName());
        assertEquals("Z3 - Mithibai Gate 2", request.getPickupGate());
        assertEquals("T1 - DN Nagar Metro Station", request.getDestinationHub());
        assertEquals("4:50 PM", request.getDepartureSlot());
        assertTrue(request.isWillingToShare());
        assertEquals("PENDING", request.getStatus());
        assertEquals(1700000000000L, request.getTimestamp());
    }

    @Test
    public void testUserProfileDefaults() {
        UserProfile userProfile = new UserProfile(
                "user_abc",
                "Test User",
                "test@university.edu",
                "",
                "Computer Science '25 • Verified Student",
                12,
                4.85,
                15.2
        );

        assertNotNull(userProfile);
        assertEquals("user_abc", userProfile.getUserId());
        assertEquals("Test User", userProfile.getDisplayName());
        assertEquals(12, userProfile.getRidesShared());
        assertEquals(4.85, userProfile.getPeerRating(), 0.001);
    }

    @Test
    public void testZoneSelectionMapping() {
        com.example.campusride.model.CampusZoneConfig.ZoneInfo z4 =
                com.example.campusride.model.CampusZoneConfig.findZoneByIdOrName("Z4");
        assertNotNull(z4);
        assertEquals("Z4", z4.getId());
        assertEquals("DJ Sanghvi Gate", z4.getName());

        com.example.campusride.model.CampusZoneConfig.ZoneInfo z7 =
                com.example.campusride.model.CampusZoneConfig.findZoneByIdOrName("Z7");
        assertNotNull(z7);
        assertEquals("Z7", z7.getId());
        assertEquals("Old Mukesh Gate", z7.getName());

        com.example.campusride.model.CampusZoneConfig.ZoneInfo t1 =
                com.example.campusride.model.CampusZoneConfig.findZoneByIdOrName("T1");
        assertNotNull(t1);
        assertEquals("T1", t1.getId());
    }

    @Test
    public void testUserProfileFcmToken() {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId("user_abc");
        userProfile.setFcmToken("sample_fcm_token_123");

        assertEquals("user_abc", userProfile.getUserId());
        assertEquals("sample_fcm_token_123", userProfile.getFcmToken());
    }
}
