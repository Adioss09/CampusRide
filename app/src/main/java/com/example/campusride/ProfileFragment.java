package com.example.campusride;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.adapter.RideRequestAdapter;
import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.model.RideRequest;
import com.example.campusride.model.UserProfile;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvAcademicInfo;
    private TextView tvRidesShared;
    private TextView tvPeerRating;
    //private TextView tvCo2Saved;
    private RecyclerView rvProfileHistory;
    private MaterialButton btnSignOut;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvAcademicInfo = view.findViewById(R.id.tvAcademicInfo);
        tvRidesShared = view.findViewById(R.id.tvRidesShared);
        tvPeerRating = view.findViewById(R.id.tvPeerRating);
        //tvCo2Saved = view.findViewById(R.id.tvCo2Saved);
        rvProfileHistory = view.findViewById(R.id.rvProfileHistory);
        btnSignOut = view.findViewById(R.id.btnSignOut);

        rvProfileHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        bindUserProfile();

        btnSignOut.setOnClickListener(v -> {
            btnSignOut.setEnabled(false);
            AuthRepository.getInstance().signOut(requireContext(), new AuthRepository.AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (isAdded()) {
                        Intent intent = new Intent(requireActivity(), SignInActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnSignOut.setEnabled(true);
                        Toast.makeText(requireContext(), "Sign-out error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        loadHistory();
    }

    private void bindUserProfile() {
        UserProfile fallbackUser = AuthRepository.getInstance().getCurrentUser();
        if (fallbackUser != null) {
            tvProfileName.setText(fallbackUser.getDisplayName());
            tvProfileEmail.setText(fallbackUser.getEmail());
            tvAcademicInfo.setText(fallbackUser.getAcademicInfo());
            tvRidesShared.setText(String.valueOf(fallbackUser.getRidesShared()));
            tvPeerRating.setText(String.format(Locale.US, "%.2f", fallbackUser.getPeerRating()));
            //tvCo2Saved.setText(String.format(Locale.US, "%.1f kg", fallbackUser.getCo2SavedKg()));

            FirestoreRepository.getInstance().getUserProfile(fallbackUser.getUserId(), new FirestoreRepository.FirestoreCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile user) {
                    if (isAdded() && user != null) {
                        tvProfileName.setText(user.getDisplayName());
                        tvProfileEmail.setText(user.getEmail());
                        tvAcademicInfo.setText(user.getAcademicInfo());
                        tvRidesShared.setText(String.valueOf(user.getRidesShared()));
                        tvPeerRating.setText(String.format(Locale.US, "%.2f", user.getPeerRating()));
                        //tvCo2Saved.setText(String.format(Locale.US, "%.1f kg", user.getCo2SavedKg()));
                    }
                }

                @Override
                public void onError(Exception e) {
                }
            });
        }
    }

    private void loadHistory() {
        FirestoreRepository.getInstance().getRideRequests(new FirestoreRepository.FirestoreCallback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                if (isAdded()) {
                    RideRequestAdapter adapter = new RideRequestAdapter(requests, request -> {
                        FirestoreRepository.getInstance().cancelRideRequest(request.getRequestId(), new FirestoreRepository.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Request cancelled", Toast.LENGTH_SHORT).show();
                                    loadHistory();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                if (isAdded()) {
                                    String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to cancel request";
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    });
                    rvProfileHistory.setAdapter(adapter);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to load ride history";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}