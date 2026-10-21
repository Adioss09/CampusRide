# CampusRide

CampusRide is a native Android campus ride-sharing application designed
to help students coordinate rides between campus gates and nearby
public-transport hubs.

## Features

-   **Firebase Authentication** for student sign-in.
-   **Google Maps integration** for viewing campus locations and transit
    hubs.
-   **Ride requests** with pickup gate, destination hub, departure slot,
    and ride-sharing preference.
-   **Ride status management** for viewing and cancelling ride requests.
-   **Profile and ride history** backed by Cloud Firestore.
-   **Campus-focused locations** for nearby university gates and transit
    connections.
-   **Teal-and-white interface** designed for a clear, student-friendly
    experience.

## Tech Stack

-   **Android:** Java, XML layouts, Android Studio
-   **Build system:** Gradle (Kotlin DSL)
-   **Backend and authentication:** Firebase Authentication, Cloud
    Firestore
-   **Maps:** Google Maps SDK for Android
-   **Intelligent demand prioritization:** Existing Python
    model/integration used by the project

## Project Structure

The repository contains the Android application and supporting project
files. Check the folders and configuration files in the repository for
the current implementation details.

## Getting Started

### Prerequisites

-   Android Studio
-   A compatible Android SDK (the app's minimum SDK is API 26)
-   Access to the Firebase project configured for CampusRide
-   A Google Maps API key with the required Maps SDK enabled

### Setup

1.  Clone the repository:

    ``` bash
    git clone https://github.com/Adioss09/CampusRide.git
    cd CampusRide
    ```

2.  Open the project in Android Studio and allow Gradle to sync.

3.  Configure Firebase for your development environment:

    -   Add the appropriate `google-services.json` file to the Android
        app module if it is required by the project configuration.
    -   Ensure Firebase Authentication and Cloud Firestore are
        configured in the Firebase project.
    -   Use the project's intended, secure Firestore rules. Do not use
        open read/write rules.

4.  Configure Google Maps:

    -   Add your own Maps SDK API key using the configuration expected
        by the project.
    -   Restrict the key appropriately in Google Cloud Console.
    -   Do not commit private credentials or local secrets.

5.  Build and run the app on an emulator or Android device.

> Setup details can vary depending on the Firebase and Maps
> configuration used by your environment. Never publish service-account
> private keys, signing keys, or other sensitive credentials.

## Firebase Notes

CampusRide uses Firebase Authentication and Cloud Firestore. Some
Firebase resources and security rules are configured in the Firebase
console and are not automatically created by cloning this repository.
Confirm that your Firebase project, authentication providers, database,
and security rules are configured before running the app.

## Development

-   Keep secrets and machine-specific configuration out of version
    control.
-   Test authentication, maps, ride creation, ride cancellation, profile
    loading, and ride history after making changes.
-   Verify backend-dependent features in the configured Firebase
    environment.

## Repository

GitHub: https://github.com/Adioss09/CampusRide

## License

No license is specified yet. Unless a license is added to this
repository, standard copyright applies and reuse is not automatically
permitted.
