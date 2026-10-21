package com.example.campusride;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.adapter.RideRequestAdapter;
import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.RideRequest;
import com.example.campusride.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class RideShareFragment extends Fragment {

    private Spinner spPickupGate;
    private Spinner spDestinationHub;
    private Spinner spDepartureSlot;
    private MaterialSwitch switchWillingToShare;
    private MaterialButton btnSubmitRequest;
    private RecyclerView rvRideRequests;

    private final List<String> gateLabels = new ArrayList<>();
    private final List<String> hubLabels = new ArrayList<>();

    private static final String[] SLOTS = {
            "4:35 PM",
            "4:50 PM",
            "5:05 PM",
            "5:20 PM"
    };

    public static RideShareFragment newInstance(String pickupZone, String destinationHub, String departureSlot) {
        RideShareFragment fragment = new RideShareFragment();
        Bundle args = new Bundle();
        if (pickupZone != null) args.putString("pickupZone", pickupZone);
        if (destinationHub != null) args.putString("destinationHub", destinationHub);
        if (departureSlot != null) args.putString("departureSlot", departureSlot);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ride_share, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spPickupGate = view.findViewById(R.id.spPickupGate);
        spDestinationHub = view.findViewById(R.id.spDestinationHub);
        spDepartureSlot = view.findViewById(R.id.spDepartureSlot);
        switchWillingToShare = view.findViewById(R.id.switchWillingToShare);
        btnSubmitRequest = view.findViewById(R.id.btnSubmitRequest);
        rvRideRequests = view.findViewById(R.id.rvRideRequests);

        setupSpinners();
        applyArguments();

        rvRideRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnSubmitRequest.setOnClickListener(v -> submitRideRequest());

        loadRideRequests();
    }

    private void setupSpinners() {
        gateLabels.clear();
        for (CampusZoneConfig.ZoneInfo z : CampusZoneConfig.getPickupZones()) {
            gateLabels.add(z.getFormattedLabel());
        }

        hubLabels.clear();
        for (CampusZoneConfig.ZoneInfo z : CampusZoneConfig.getTransitHubs()) {
            hubLabels.add(z.getFormattedLabel());
        }

        ArrayAdapter<String> gateAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner, gateLabels);
        gateAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spPickupGate.setAdapter(gateAdapter);

        ArrayAdapter<String> hubAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner, hubLabels);
        hubAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spDestinationHub.setAdapter(hubAdapter);

        ArrayAdapter<String> slotAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner, SLOTS);
        slotAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spDepartureSlot.setAdapter(slotAdapter);
    }

    private void applyArguments() {
        Bundle args = getArguments();
        if (args == null) return;

        String pickupZone = args.getString("pickupZone");
        if (!TextUtils.isEmpty(pickupZone)) {
            spPickupGate.post(() -> selectSpinnerMatching(spPickupGate, pickupZone));
        }

        String destinationHub = args.getString("destinationHub");
        if (!TextUtils.isEmpty(destinationHub)) {
            spDestinationHub.post(() -> selectSpinnerMatching(spDestinationHub, destinationHub));
        }

        String departureSlot = args.getString("departureSlot");
        if (!TextUtils.isEmpty(departureSlot)) {
            spDepartureSlot.post(() -> selectSpinnerMatching(spDepartureSlot, departureSlot));
        }
    }

    private void selectSpinnerMatching(Spinner spinner, String target) {
        if (spinner == null || spinner.getAdapter() == null || target == null) return;
        String cleanTarget = target.trim();
        CampusZoneConfig.ZoneInfo zone = CampusZoneConfig.findZoneByIdOrName(cleanTarget);

        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            String item = spinner.getAdapter().getItem(i).toString();
            if (zone != null) {
                if (item.contains(zone.getId()) || item.contains(zone.getName())) {
                    spinner.setSelection(i);
                    return;
                }
            } else if (item.equalsIgnoreCase(cleanTarget) || item.contains(cleanTarget) || cleanTarget.contains(item)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void submitRideRequest() {
        btnSubmitRequest.setEnabled(false);

        FirebaseUser fbUser = AuthRepository.getInstance().getFirebaseUser();
        if (fbUser == null) {
            btnSubmitRequest.setEnabled(true);
            Toast.makeText(requireContext(), "Please sign in to submit ride requests.", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfile user = AuthRepository.getInstance().getCurrentUser();
        String userId = fbUser.getUid();
        String userName = (user != null && !TextUtils.isEmpty(user.getDisplayName())) ? user.getDisplayName() : fbUser.getDisplayName();
        if (TextUtils.isEmpty(userName)) {
            userName = "Campus Student";
        }

        String pickup = (String) spPickupGate.getSelectedItem();
        String destination = (String) spDestinationHub.getSelectedItem();
        String slot = (String) spDepartureSlot.getSelectedItem();
        boolean share = switchWillingToShare.isChecked();

        RideRequest request = new RideRequest(
                null,
                userId,
                userName,
                pickup,
                destination,
                slot,
                share,
                "PENDING",
                System.currentTimeMillis()
        );

        FirestoreRepository.getInstance().saveRideRequest(request, new FirestoreRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    btnSubmitRequest.setEnabled(true);
                    Toast.makeText(requireContext(), "Ride-sharing request saved successfully!", Toast.LENGTH_SHORT).show();
                    loadRideRequests();
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    btnSubmitRequest.setEnabled(true);
                    String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to save request";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadRideRequests() {
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
                                    loadRideRequests();
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
                    rvRideRequests.setAdapter(adapter);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to load requests";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}