package com.example.campusride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.notification.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private boolean isProgrammaticNavigation = false;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handle permission result gracefully
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthRepository.getInstance().isUserSignedIn()) {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isProgrammaticNavigation) {
                return true;
            }

            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.navigation_ride_share) {
                selectedFragment = new RideShareFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Initialize Notification Channel & Sync FCM Token
        NotificationHelper.createNotificationChannel(this);
        syncFcmToken();
        checkNotificationPermission();

        // Set default fragment or handle notification navigation intent
        if (savedInstanceState == null) {
            if (!handleNotificationIntent(getIntent())) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            }
        } else {
            handleNotificationIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private boolean handleNotificationIntent(Intent intent) {
        if (intent != null && "ride_share".equals(intent.getStringExtra("navigate_to"))) {
            navigateToRideShare(null, null, null);
            return true;
        }
        return false;
    }

    private void syncFcmToken() {
        FirebaseUser user = AuthRepository.getInstance().getFirebaseUser();
        if (user != null) {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                if (!TextUtils.isEmpty(token)) {
                    FirestoreRepository.getInstance().saveFcmToken(user.getUid(), token, null);
                }
            });
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    public void navigateToRideShare(String pickupZone, String destinationHub, String departureSlot) {
        isProgrammaticNavigation = true;
        RideShareFragment fragment = RideShareFragment.newInstance(pickupZone, destinationHub, departureSlot);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_ride_share);
        }
        isProgrammaticNavigation = false;
    }
}