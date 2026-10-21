package com.example.campusride.data;

import android.content.Context;
import android.text.TextUtils;

import com.example.campusride.model.UserProfile;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Repository handling Firebase Authentication with Google Sign-In.
 */
public class AuthRepository {

    public interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private static AuthRepository instance;
    private final FirebaseAuth firebaseAuth;

    private AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public FirebaseUser getFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    public UserProfile getCurrentUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return null;
        }

        String displayName = user.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = "Campus Student";
        }

        String email = user.getEmail();
        if (TextUtils.isEmpty(email)) {
            email = "student@university.edu";
        }

        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        return new UserProfile(
                user.getUid(),
                displayName,
                email,
                photoUrl,
                "Computer Science '25 • Verified Student",
                34,
                4.96,
                18.4
        );
    }

    public GoogleSignInClient getGoogleSignInClient(Context context) {
        String webClientId = getWebClientId(context);

        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (!TextUtils.isEmpty(webClientId)) {
            gsoBuilder.requestIdToken(webClientId);
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build());
    }

    public String getWebClientId(Context context) {
        int resId = context.getResources().getIdentifier("default_web_client_id", "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return "";
    }

    public void firebaseAuthWithGoogle(String idToken, AuthCallback<UserProfile> callback) {
        if (TextUtils.isEmpty(idToken)) {
            if (callback != null) {
                callback.onError(new Exception("Google ID token is missing. Web Client ID needs to be configured in Firebase Console."));
            }
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        UserProfile profile = getCurrentUser();
                        if (profile != null) {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(token -> {
                                        if (!TextUtils.isEmpty(token)) {
                                            FirestoreRepository.getInstance().saveFcmToken(profile.getUserId(), token, null);
                                        }
                                    });

                            FirestoreRepository.getInstance().getUserProfile(profile.getUserId(), new FirestoreRepository.FirestoreCallback<UserProfile>() {
                                @Override
                                public void onSuccess(UserProfile userProfile) {
                                    if (callback != null) {
                                        callback.onSuccess(userProfile != null ? userProfile : profile);
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (callback != null) {
                                        callback.onSuccess(profile);
                                    }
                                }
                            });
                        } else if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException() != null ? task.getException() : new Exception("Authentication failed"));
                        }
                    }
                });
    }

    public void signOut(Context context, AuthCallback<Void> callback) {
        try {
            FirebaseUser currentFbUser = getFirebaseUser();
            if (currentFbUser != null) {
                FirestoreRepository.getInstance().clearFcmToken(currentFbUser.getUid(), null);
            }

            firebaseAuth.signOut();

            GoogleSignInClient googleSignInClient = getGoogleSignInClient(context);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
}