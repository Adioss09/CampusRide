# CampusRide

CampusRide is a native Android campus ride-sharing application designed
to help students coordinate rides between campus gates and nearby
public-transport hubs, featuring real-time driver dispatching and map-first navigation.

## Features

-   **Firebase Authentication** for student and driver sign-in.
-   **Google Maps Integration** for viewing campus pickup gates, transit hubs, and route previews.
-   **Flexible Ride Requests** with pickup gate, destination hub, native time-picker departure selection, and carpool sharing preference.
-   **Real-Time Driver Console & Map-First Dispatch** featuring live pending ride streams, concurrency-safe atomic transactions for claiming rides, rejection filtering, and Google Maps turn-by-turn driving directions intent.
-   **Real-Time Student Ride Status** updates via Cloud Firestore snapshot listeners (`PENDING`, `MATCHED / Driver Assigned`, `CANCELLED`).
-   **Push Notifications Infrastructure** powered by Firebase Cloud Messaging (FCM) and Cloud Functions v2.
-   **Profile and Ride History** backed by Cloud Firestore.
-   **Teal-and-white Interface** designed for high contrast, accessibility, and clean student/driver separation.

## Tech Stack

-   **Android:** Java, XML layouts, Material Design 3, Android Studio
-   **Build system:** Gradle (Kotlin DSL)
-   **Backend & Authentication:** Firebase Authentication, Cloud Firestore
-   **Push Notifications:** Firebase Cloud Messaging (FCM) & Node.js Cloud Functions v2
-   **Maps & Navigation:** Google Maps SDK for Android & Google Maps Driving Directions Intent

## Getting Started

### Prerequisites

-   Android Studio
-   A compatible Android SDK (minimum SDK API 26)
-   Access to the Firebase project configured for CampusRide (`campusride-126f0`)
-   A Google Maps API key with Maps SDK for Android enabled

### Setup

1.  Clone the repository:

    ``` bash
    git clone https://github.com/Adioss09/CampusRide.git
    cd CampusRide
    ```

2.  Open the project in Android Studio and allow Gradle to sync.

3.  Configure Firebase for your development environment:

    -   Ensure `app/google-services.json` is present.
    -   Configure Firebase Authentication (Google Sign-In) and Cloud Firestore in Firebase Console.
    -   Deploy security rules from `firestore.rules`.
    -   To designate a driver account, create document `driver_roles/{driver_uid}` with field `isDriver = true` in Firestore (or set field `role = "driver"` in `users/{driver_uid}`).

4.  Configure Google Maps:

    -   Ensure your Maps SDK API key is set in `res/values/strings.xml` or local properties.
    -   Restrict key in Google Cloud Console appropriately.

5.  Build and run the app on an emulator or Android device.

## Repository

GitHub: https://github.com/Adioss09/CampusRide

## License

No license is specified yet.
