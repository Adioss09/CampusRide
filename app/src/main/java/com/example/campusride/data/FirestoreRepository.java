package com.example.campusride.data;

import android.text.TextUtils;

import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.GateDemand;
import com.example.campusride.model.RideRequest;
import com.example.campusride.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clean integration point for Cloud Firestore operations (ride requests, user profiles, gate demands, transit hubs, driver workflows).
 */
public class FirestoreRepository {

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class TransitHub {
        private final String id;
        private final String name;
        private final double latitude;
        private final double longitude;

        public TransitHub(String id, String name, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }

    private static final String RIDE_REQUESTS_COLLECTION = "ride_requests";
    private static final String USERS_COLLECTION = "users";

    private static FirestoreRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirestoreRepository getInstance() {
        if (instance == null) {
            instance = new FirestoreRepository();
        }
        return instance;
    }

    /**
     * Saves a ride request to Cloud Firestore under 'ride_requests'.
     * Uses actual authenticated Firebase User UID, stable document ID, and prevents duplicates.
     */
    public void saveRideRequest(RideRequest request, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Authentication required to submit a ride request."));
            }
            return;
        }

        if (request == null) {
            if (callback != null) {
                callback.onError(new Exception("Invalid ride request data."));
            }
            return;
        }

        // Always enforce actual authenticated Firebase UID
        request.setUserId(currentUser.getUid());

        if (request.getTimestamp() <= 0) {
            request.setTimestamp(System.currentTimeMillis());
        }

        if (TextUtils.isEmpty(request.getStatus())) {
            request.setStatus("PENDING");
        }

        // Check for duplicate pending submission for same route and slot
        db.collection(RIDE_REQUESTS_COLLECTION)
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        RideRequest existing = doc.toObject(RideRequest.class);
                        if (existing != null
                                && TextUtils.equals(existing.getPickupGate(), request.getPickupGate())
                                && TextUtils.equals(existing.getDestinationHub(), request.getDestinationHub())
                                && TextUtils.equals(existing.getDepartureSlot(), request.getDepartureSlot())) {
                            if (callback != null) {
                                callback.onError(new Exception("A pending ride request for this route and slot already exists."));
                            }
                            return;
                        }
                    }

                    // Assign stable document ID if missing or temporary
                    DocumentReference docRef;
                    if (TextUtils.isEmpty(request.getRequestId()) || request.getRequestId().startsWith("req_")) {
                        docRef = db.collection(RIDE_REQUESTS_COLLECTION).document();
                        request.setRequestId(docRef.getId());
                    } else {
                        docRef = db.collection(RIDE_REQUESTS_COLLECTION).document(request.getRequestId());
                    }

                    docRef.set(request)
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onError(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Reads actual persisted ride request documents belonging to the authenticated user from Cloud Firestore.
     */
    public void getRideRequests(FirestoreCallback<List<RideRequest>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Authentication required to view ride requests."));
            }
            return;
        }

        db.collection(RIDE_REQUESTS_COLLECTION)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RideRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        RideRequest req = doc.toObject(RideRequest.class);
                        if (req != null) {
                            if (TextUtils.isEmpty(req.getRequestId())) {
                                req.setRequestId(doc.getId());
                            }
                            requests.add(req);
                        }
                    }
                    // Sort descending by timestamp
                    Collections.sort(requests, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                    if (callback != null) {
                        callback.onSuccess(requests);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Real-time Firestore snapshot listener for a student's ride requests.
     */
    public ListenerRegistration listenToUserRideRequests(String userId, FirestoreCallback<List<RideRequest>> callback) {
        return db.collection(RIDE_REQUESTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error);
                        return;
                    }

                    List<RideRequest> requests = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            RideRequest req = doc.toObject(RideRequest.class);
                            if (req != null) {
                                if (TextUtils.isEmpty(req.getRequestId())) {
                                    req.setRequestId(doc.getId());
                                }
                                requests.add(req);
                            }
                        }
                    }
                    Collections.sort(requests, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    if (callback != null) {
                        callback.onSuccess(requests);
                    }
                });
    }

    /**
     * Cancels an eligible ride request belonging to the authenticated user in Cloud Firestore.
     */
    public void cancelRideRequest(String requestId, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Authentication required to cancel ride request."));
            }
            return;
        }

        if (TextUtils.isEmpty(requestId)) {
            if (callback != null) {
                callback.onError(new Exception("Invalid request ID for cancellation."));
            }
            return;
        }

        DocumentReference docRef = db.collection(RIDE_REQUESTS_COLLECTION).document(requestId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                if (callback != null) {
                    callback.onError(new Exception("Ride request not found in database."));
                }
                return;
            }

            RideRequest existingRequest = documentSnapshot.toObject(RideRequest.class);
            if (existingRequest == null) {
                if (callback != null) {
                    callback.onError(new Exception("Corrupted ride request record."));
                }
                return;
            }

            // Verify ownership in data-access flow
            if (!currentUser.getUid().equals(existingRequest.getUserId())) {
                if (callback != null) {
                    callback.onError(new Exception("Unauthorized: You can only cancel your own ride requests."));
                }
                return;
            }

            if ("CANCELLED".equalsIgnoreCase(existingRequest.getStatus())) {
                if (callback != null) {
                    callback.onError(new Exception("Ride request is already cancelled."));
                }
                return;
            }

            docRef.update("status", "CANCELLED")
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onError(e);
            }
        });
    }

    /**
     * Gets user profile from Cloud Firestore 'users' collection or persists initial profile.
     */
    public void getUserProfile(String userId, FirestoreCallback<UserProfile> callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) {
                callback.onError(new Exception("Invalid user ID."));
            }
            return;
        }

        db.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserProfile profile = doc.toObject(UserProfile.class);
                        if (profile != null) {
                            if (callback != null) {
                                callback.onSuccess(profile);
                            }
                            return;
                        }
                    }

                    // Create default profile for newly signed in user
                    UserProfile initialProfile = AuthRepository.getInstance().getCurrentUser();
                    if (initialProfile == null) {
                        initialProfile = new UserProfile(userId, "Campus Student", "student@university.edu", "", "Computer Science '25 • Verified Student", 0, 5.00, 0.0);
                    }
                    final UserProfile defaultProfile = initialProfile;
                    saveUserProfile(defaultProfile, new FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (callback != null) {
                                callback.onSuccess(defaultProfile);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (callback != null) {
                                callback.onSuccess(defaultProfile);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Saves user profile to Cloud Firestore 'users' collection.
     */
    public void saveUserProfile(UserProfile profile, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Authentication required to save profile."));
            }
            return;
        }

        if (profile == null || TextUtils.isEmpty(profile.getUserId())) {
            if (callback != null) {
                callback.onError(new Exception("Invalid user profile."));
            }
            return;
        }

        if (!currentUser.getUid().equals(profile.getUserId())) {
            if (callback != null) {
                callback.onError(new Exception("Unauthorized: You can only update your own user profile."));
            }
            return;
        }

        db.collection(USERS_COLLECTION).document(profile.getUserId())
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Updates FCM registration token for authenticated user in Firestore 'users' collection without overwriting existing profile data.
     */
    public void saveFcmToken(String userId, String token, FirestoreCallback<Void> callback) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(token)) {
            if (callback != null) {
                callback.onError(new Exception("Invalid user ID or FCM token."));
            }
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.getUid().equals(userId)) {
            if (callback != null) {
                callback.onError(new Exception("Unauthorized: Cannot update FCM token for another user."));
            }
            return;
        }

        db.collection(USERS_COLLECTION).document(userId)
                .set(Collections.singletonMap("fcmToken", token), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Clears FCM registration token for user upon sign-out to prevent incorrect notification routing.
     */
    public void clearFcmToken(String userId, FirestoreCallback<Void> callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }

        db.collection(USERS_COLLECTION).document(userId)
                .set(Collections.singletonMap("fcmToken", ""), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    // ==========================================
    // DRIVER WORKFLOW METHODS
    // ==========================================

    /**
     * Checks if the user is an authorized driver in Firestore.
     */
    public void checkIsDriver(String userId, FirestoreCallback<Boolean> callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) callback.onSuccess(false);
            return;
        }

        // 1. Check protected collection 'driver_roles/{userId}'
        db.collection("driver_roles").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isDriver = doc.getBoolean("isDriver");
                        if (isDriver != null && isDriver) {
                            if (callback != null) callback.onSuccess(true);
                            return;
                        }
                    }

                    // 2. Fallback check 'users/{userId}' document for role == "driver"
                    db.collection(USERS_COLLECTION).document(userId).get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    String role = userDoc.getString("role");
                                    if ("driver".equalsIgnoreCase(role)) {
                                        if (callback != null) callback.onSuccess(true);
                                        return;
                                    }
                                }
                                if (callback != null) callback.onSuccess(false);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onSuccess(false);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onSuccess(false);
                });
    }

    /**
     * Listens to real-time incoming pending ride requests for driver dashboard.
     * Filters out requests that were rejected by this driver.
     */
    public ListenerRegistration listenToPendingRideRequests(String driverId, FirestoreCallback<List<RideRequest>> callback) {
        return db.collection(RIDE_REQUESTS_COLLECTION)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error);
                        return;
                    }

                    List<RideRequest> requests = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            RideRequest req = doc.toObject(RideRequest.class);
                            if (req != null) {
                                if (TextUtils.isEmpty(req.getRequestId())) {
                                    req.setRequestId(doc.getId());
                                }
                                List<String> rejected = req.getRejectedDriverIds();
                                if (driverId != null && rejected != null && rejected.contains(driverId)) {
                                    continue;
                                }
                                requests.add(req);
                            }
                        }
                    }
                    Collections.sort(requests, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    if (callback != null) {
                        callback.onSuccess(requests);
                    }
                });
    }

    /**
     * Atomically claims a ride request for the driver using a Firestore transaction.
     * Guarantees concurrency safety: prevents two drivers from accepting the same ride.
     */
    public void acceptRideRequestTransaction(String requestId, String driverId, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(driverId) || !currentUser.getUid().equals(driverId)) {
            if (callback != null) callback.onError(new Exception("Driver authentication required."));
            return;
        }

        if (TextUtils.isEmpty(requestId)) {
            if (callback != null) callback.onError(new Exception("Invalid request ID."));
            return;
        }

        DocumentReference docRef = db.collection(RIDE_REQUESTS_COLLECTION).document(requestId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Ride request document does not exist.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String currentStatus = snapshot.getString("status");
            if (!"PENDING".equalsIgnoreCase(currentStatus)) {
                throw new FirebaseFirestoreException("Ride is no longer available (already accepted or cancelled).", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(docRef, "status", "MATCHED");
            transaction.update(docRef, "driverId", driverId);
            transaction.update(docRef, "acceptedAt", System.currentTimeMillis());

            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e);
        });
    }

    /**
     * Records a driver's rejection for a request without cancelling or deleting the ride for the student.
     */
    public void rejectRideRequest(String requestId, String driverId, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(driverId) || !currentUser.getUid().equals(driverId)) {
            if (callback != null) callback.onError(new Exception("Driver authentication required."));
            return;
        }

        if (TextUtils.isEmpty(requestId)) {
            if (callback != null) callback.onError(new Exception("Invalid request ID."));
            return;
        }

        DocumentReference docRef = db.collection(RIDE_REQUESTS_COLLECTION).document(requestId);
        docRef.update("rejectedDriverIds", FieldValue.arrayUnion(driverId))
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    /**
     * Listens to real-time status updates for rides accepted by this driver.
     */
    public ListenerRegistration listenToDriverAcceptedRides(String driverId, FirestoreCallback<List<RideRequest>> callback) {
        return db.collection(RIDE_REQUESTS_COLLECTION)
                .whereEqualTo("driverId", driverId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error);
                        return;
                    }

                    List<RideRequest> requests = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            RideRequest req = doc.toObject(RideRequest.class);
                            if (req != null) {
                                if (TextUtils.isEmpty(req.getRequestId())) {
                                    req.setRequestId(doc.getId());
                                }
                                requests.add(req);
                            }
                        }
                    }
                    Collections.sort(requests, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    if (callback != null) {
                        callback.onSuccess(requests);
                    }
                });
    }

    public void getCampusGateDemands(FirestoreCallback<List<GateDemand>> callback) {
        List<GateDemand> mockGates = new ArrayList<>();
        List<CampusZoneConfig.ZoneInfo> zones = CampusZoneConfig.getPickupZones();

        for (CampusZoneConfig.ZoneInfo z : zones) {
            if ("Z3".equalsIgnoreCase(z.getId())) {
                mockGates.add(new GateDemand(z.getId(), z.getName(), z.getLatitude(), z.getLongitude(), 220, 3, 2.0, 0.91, "CRITICAL", "Illustrative timetable estimate: High student outflow with imminent class dismissal."));
            } else if ("Z4".equalsIgnoreCase(z.getId()) || "Z6".equalsIgnoreCase(z.getId())) {
                mockGates.add(new GateDemand(z.getId(), z.getName(), z.getLatitude(), z.getLongitude(), 140, 8, 5.0, 0.75, "HIGH", "Illustrative timetable estimate: High outflow with approaching dismissal."));
            } else if ("Z1".equalsIgnoreCase(z.getId()) || "Z5".equalsIgnoreCase(z.getId())) {
                mockGates.add(new GateDemand(z.getId(), z.getName(), z.getLatitude(), z.getLongitude(), 80, 15, 8.0, 0.48, "MEDIUM", "Illustrative timetable estimate: Steady movement near campus plaza."));
            } else {
                mockGates.add(new GateDemand(z.getId(), z.getName(), z.getLatitude(), z.getLongitude(), 30, 30, 15.0, 0.20, "LOW", "Illustrative timetable estimate: Low student outflow."));
            }
        }

        if (callback != null) {
            callback.onSuccess(mockGates);
        }
    }

    public void getTransitHubs(FirestoreCallback<List<TransitHub>> callback) {
        List<TransitHub> hubs = new ArrayList<>();
        for (CampusZoneConfig.ZoneInfo z : CampusZoneConfig.getTransitHubs()) {
            hubs.add(new TransitHub(z.getId(), z.getName(), z.getLatitude(), z.getLongitude()));
        }
        if (callback != null) {
            callback.onSuccess(hubs);
        }
    }
}