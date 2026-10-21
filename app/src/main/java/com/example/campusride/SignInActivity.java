package com.example.campusride;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusride.data.AuthRepository;
import com.example.campusride.model.UserProfile;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

public class SignInActivity extends AppCompatActivity {

    private AuthRepository authRepo;
    private MaterialButton btnGoogleSignIn;
    private ProgressBar pbLoading;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            String idToken = account.getIdToken();
                            handleFirebaseSignInWithToken(idToken);
                        } else {
                            showLoading(false);
                            Toast.makeText(this, "Google sign-in account details missing.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ApiException e) {
                        showLoading(false);
                        Toast.makeText(this, "Google sign-in failed (code " + e.getStatusCode() + "): " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    showLoading(false);
                    Toast.makeText(this, "Google sign-in cancelled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authRepo = AuthRepository.getInstance();
        if (authRepo.isUserSignedIn()) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_sign_in);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        pbLoading = findViewById(R.id.pbLoading);

        btnGoogleSignIn.setOnClickListener(v -> launchGoogleSignIn());
    }

    private void launchGoogleSignIn() {
        showLoading(true);

        String webClientId = authRepo.getWebClientId(this);
        if (TextUtils.isEmpty(webClientId)) {
            showLoading(false);
            Toast.makeText(this, "Web Client ID not found in google-services.json. Please ensure OAuth Web Client is configured in Firebase Console.", Toast.LENGTH_LONG).show();
            return;
        }

        GoogleSignInClient signInClient = authRepo.getGoogleSignInClient(this);
        googleSignInLauncher.launch(signInClient.getSignInIntent());
    }

    private void handleFirebaseSignInWithToken(String idToken) {
        authRepo.firebaseAuthWithGoogle(idToken, new AuthRepository.AuthCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile user) {
                showLoading(false);
                Toast.makeText(SignInActivity.this, "Welcome, " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                startMainActivity();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(SignInActivity.this, "Firebase Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setEnabled(!loading);
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}