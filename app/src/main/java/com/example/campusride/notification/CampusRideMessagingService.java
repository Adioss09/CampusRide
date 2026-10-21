package com.example.campusride.notification;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.model.UserProfile;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class CampusRideMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        UserProfile user = AuthRepository.getInstance().getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            FirestoreRepository.getInstance().saveFcmToken(user.getUserId(), token, null);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;
        String requestId = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (TextUtils.isEmpty(title) && data.containsKey("title")) {
                title = data.get("title");
            }
            if (TextUtils.isEmpty(body) && data.containsKey("body")) {
                body = data.get("body");
            }
            if (data.containsKey("requestId")) {
                requestId = data.get("requestId");
            }
        }

        if (TextUtils.isEmpty(title)) {
            title = "CampusRide Update";
        }
        if (TextUtils.isEmpty(body)) {
            body = "Your ride status has been updated.";
        }

        NotificationHelper.showNotification(getApplicationContext(), title, body, requestId);
    }
}
