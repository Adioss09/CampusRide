const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

/**
 * Cloud Function triggered when a ride request status changes in Firestore.
 * Sends an FCM push notification to the user via Firebase Admin SDK.
 */
exports.onRideRequestStatusUpdated = onDocumentUpdated("ride_requests/{requestId}", async (event) => {
  const beforeData = event.data.before ? event.data.before.data() : null;
  const afterData = event.data.after ? event.data.after.data() : null;

  if (!beforeData || !afterData) {
    return;
  }

  const oldStatus = beforeData.status;
  const newStatus = afterData.status;

  // Idempotency check: ignore if status did not change
  if (oldStatus === newStatus) {
    return;
  }

  const pickup = afterData.pickupGate || "Campus";
  const destination = afterData.destinationHub || "Destination";

  let title = "";
  let body = "";

  if (newStatus === "MATCHED" || newStatus === "CONFIRMED") {
    title = "Ride Confirmed!";
    body = `Your CampusRide from ${pickup} to ${destination} has been confirmed!`;
  } else if (newStatus === "CANCELLED") {
    title = "Ride Cancelled";
    body = `Your CampusRide from ${pickup} to ${destination} has been cancelled.`;
  } else if (newStatus === "COMPLETED") {
    title = "Ride Completed";
    body = "Your CampusRide is complete. Thank you for riding with CampusRide!";
  } else {
    // Unrelated transitions (e.g. initial PENDING creation) do not trigger push notifications
    return;
  }

  const userId = afterData.userId;
  if (!userId) {
    console.log("No userId present in ride request:", event.params.requestId);
    return;
  }

  // Retrieve recipient's registered FCM token from Firestore users collection
  const db = getFirestore();
  const userDoc = await db.collection("users").doc(userId).get();

  if (!userDoc.exists) {
    console.log("User document not found for userId:", userId);
    return;
  }

  const userData = userDoc.data();
  const fcmToken = userData ? userData.fcmToken : null;

  if (!fcmToken || fcmToken.trim() === "") {
    console.log("No active FCM token found for user:", userId);
    return;
  }

  const message = {
    token: fcmToken,
    notification: {
      title: title,
      body: body,
    },
    data: {
      requestId: event.params.requestId || "",
      pickupGate: pickup,
      destinationHub: destination,
      status: newStatus,
      navigate_to: "ride_share",
    },
    android: {
      priority: "high",
      notification: {
        channelId: "campusride_notifications",
      },
    },
  };

  try {
    const response = await getMessaging().send(message);
    console.log("Successfully sent FCM notification:", response);
  } catch (error) {
    console.error("Error sending FCM notification:", error);
    if (
      error.code === "messaging/invalid-registration-token" ||
      error.code === "messaging/registration-token-not-registered"
    ) {
      console.log("Clearing invalid token for user:", userId);
      await db.collection("users").doc(userId).update({ fcmToken: "" });
    }
  }
});
