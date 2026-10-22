package com.example.campusride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.adapter.DriverRideAdapter;
import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.RideRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class DriverDashboardFragment extends Fragment implements OnMapReadyCallback {

    private TextView tvPendingCount;
    private TextView tvAcceptedCount;
    private TextView tvEmptyState;
    private MaterialButton btnTogglePending;
    private MaterialButton btnToggleAccepted;
    private ProgressBar pbDriverLoading;
    private RecyclerView rvDriverRequests;

    private GoogleMap googleMap;
    private ListenerRegistration pendingListener;
    private ListenerRegistration acceptedListener;

    private final List<RideRequest> pendingRequestsList = new ArrayList<>();
    private final List<RideRequest> acceptedRequestsList = new ArrayList<>();
    private RideRequest selectedRequest;
    private boolean showingPending = true;

    private static final LatLng MAIN_CAMPUS_CENTER = new LatLng(19.105, 72.837);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvAcceptedCount = view.findViewById(R.id.tvAcceptedCount);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnTogglePending = view.findViewById(R.id.btnTogglePending);
        btnToggleAccepted = view.findViewById(R.id.btnToggleAccepted);
        pbDriverLoading = view.findViewById(R.id.pbDriverLoading);
        rvDriverRequests = view.findViewById(R.id.rvDriverRequests);

        rvDriverRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnTogglePending.setOnClickListener(v -> {
            showingPending = true;
            updateListDisplay();
        });

        btnToggleAccepted.setOnClickListener(v -> {
            showingPending = false;
            updateListDisplay();
        });

        initMap();
        startRealtimeListeners();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.driverMapContainer);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.driverMapContainer, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        try {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAIN_CAMPUS_CENTER, 14.8f));
            renderMapForSelectedRequest();
        } catch (Exception e) {
        }
    }

    private void renderMapForSelectedRequest() {
        if (googleMap == null) return;

        googleMap.clear();

        if (selectedRequest == null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(MAIN_CAMPUS_CENTER, 14.8f));
            return;
        }

        CampusZoneConfig.ZoneInfo pickupZone = CampusZoneConfig.findZoneByIdOrName(selectedRequest.getPickupGate());
        CampusZoneConfig.ZoneInfo destZone = CampusZoneConfig.findZoneByIdOrName(selectedRequest.getDestinationHub());

        LatLng pickupPos = pickupZone != null ? new LatLng(pickupZone.getLatitude(), pickupZone.getLongitude()) : null;
        LatLng destPos = destZone != null ? new LatLng(destZone.getLatitude(), destZone.getLongitude()) : null;

        if (pickupPos != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(pickupPos)
                    .title("Pickup: " + selectedRequest.getPickupGate())
                    .snippet("Departure Slot: " + selectedRequest.getDepartureSlot())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (destPos != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(destPos)
                    .title("Destination: " + selectedRequest.getDestinationHub())
                    .snippet("Terminal Hub")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }

        if (pickupPos != null && destPos != null) {
            googleMap.addPolyline(new PolylineOptions()
                    .add(pickupPos)
                    .add(destPos)
                    .color(ContextCompat.getColor(requireContext(), R.color.primary))
                    .width(8f));

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupPos)
                    .include(destPos)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
        } else if (pickupPos != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupPos, 15.5f));
        }
    }

    private void startRealtimeListeners() {
        FirebaseUser user = AuthRepository.getInstance().getFirebaseUser();
        if (user == null) {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Driver authentication required.");
            }
            return;
        }

        String driverId = user.getUid();
        pbDriverLoading.setVisibility(View.VISIBLE);

        stopListeners();

        pendingListener = FirestoreRepository.getInstance().listenToPendingRideRequests(driverId, new FirestoreRepository.FirestoreCallback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                if (isAdded()) {
                    pbDriverLoading.setVisibility(View.GONE);
                    pendingRequestsList.clear();
                    if (requests != null) {
                        pendingRequestsList.addAll(requests);
                    }
                    tvPendingCount.setText(String.valueOf(pendingRequestsList.size()));

                    if (showingPending) {
                        if (!pendingRequestsList.isEmpty()) {
                            if (selectedRequest == null || !pendingRequestsList.contains(selectedRequest)) {
                                selectedRequest = pendingRequestsList.get(0);
                            }
                        } else {
                            selectedRequest = null;
                        }
                        renderMapForSelectedRequest();
                    }
                    updateListDisplay();
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    pbDriverLoading.setVisibility(View.GONE);
                    String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Error loading pending requests";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        acceptedListener = FirestoreRepository.getInstance().listenToDriverAcceptedRides(driverId, new FirestoreRepository.FirestoreCallback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                if (isAdded()) {
                    acceptedRequestsList.clear();
                    if (requests != null) {
                        acceptedRequestsList.addAll(requests);
                    }
                    tvAcceptedCount.setText(String.valueOf(acceptedRequestsList.size()));

                    if (!showingPending) {
                        if (!acceptedRequestsList.isEmpty()) {
                            if (selectedRequest == null || !acceptedRequestsList.contains(selectedRequest)) {
                                selectedRequest = acceptedRequestsList.get(0);
                            }
                        } else {
                            selectedRequest = null;
                        }
                        renderMapForSelectedRequest();
                    }
                    updateListDisplay();
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void updateListDisplay() {
        List<RideRequest> displayList = showingPending ? pendingRequestsList : acceptedRequestsList;

        if (displayList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(showingPending ? "No incoming ride requests at the moment." : "You have not accepted any rides yet.");
            rvDriverRequests.setAdapter(null);
            selectedRequest = null;
            renderMapForSelectedRequest();
        } else {
            tvEmptyState.setVisibility(View.GONE);
            FirebaseUser user = AuthRepository.getInstance().getFirebaseUser();
            String driverId = user != null ? user.getUid() : "";

            DriverRideAdapter adapter = new DriverRideAdapter(displayList, new DriverRideAdapter.OnActionListener() {
                @Override
                public void onAcceptClick(RideRequest request) {
                    selectedRequest = request;
                    renderMapForSelectedRequest();

                    FirestoreRepository.getInstance().acceptRideRequestTransaction(request.getRequestId(), driverId, new FirestoreRepository.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Ride accepted successfully!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (isAdded()) {
                                String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to accept ride";
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                                updateListDisplay();
                            }
                        }
                    });
                }

                @Override
                public void onRejectClick(RideRequest request) {
                    selectedRequest = request;
                    renderMapForSelectedRequest();

                    FirestoreRepository.getInstance().rejectRideRequest(request.getRequestId(), driverId, new FirestoreRepository.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Request hidden from dashboard", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (isAdded()) {
                                String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to reject request";
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                                updateListDisplay();
                            }
                        }
                    });
                }
            });
            rvDriverRequests.setAdapter(adapter);
        }
    }

    private void stopListeners() {
        if (pendingListener != null) {
            pendingListener.remove();
            pendingListener = null;
        }
        if (acceptedListener != null) {
            acceptedListener.remove();
            acceptedListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListeners();
    }
}
