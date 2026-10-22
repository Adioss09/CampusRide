package com.example.campusride;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.example.campusride.adapter.GateDemandAdapter;
import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.GateDemand;
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

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private RecyclerView rvMapGates;
    private TextView tvMapGatesHeader;
    private ProgressBar pbMapLoading;
    private MaterialButton btnMainCampus;
    private MaterialButton btnFocusZone7;
    private View mapCard;

    // Driver Action Card Views
    private View cardDriverAction;
    private TextView tvDriverMapRoute;
    private TextView tvDriverMapSlot;
    private MaterialButton btnDriverMapAccept;
    private MaterialButton btnDriverMapNavigate;
    private MaterialButton btnDriverMapReject;

    private GoogleMap googleMap;
    private List<GateDemand> loadedGates;
    private List<FirestoreRepository.TransitHub> loadedHubs;

    private ListenerRegistration driverPendingListener;
    private final List<RideRequest> driverPendingRequests = new ArrayList<>();
    private RideRequest currentDriverSelectedRequest;
    private boolean isDriverUser = false;

    private static final LatLng MAIN_CAMPUS_CENTER = new LatLng(19.105, 72.837);
    private static final LatLng ZONE_7_CENTER = new LatLng(19.109682, 72.837240);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMapGates = view.findViewById(R.id.rvMapGates);
        tvMapGatesHeader = view.findViewById(R.id.tvMapGatesHeader);
        pbMapLoading = view.findViewById(R.id.pbMapLoading);
        btnMainCampus = view.findViewById(R.id.btnMainCampus);
        btnFocusZone7 = view.findViewById(R.id.btnFocusZone7);
        mapCard = view.findViewById(R.id.mapCard);

        cardDriverAction = view.findViewById(R.id.cardDriverAction);
        tvDriverMapRoute = view.findViewById(R.id.tvDriverMapRoute);
        tvDriverMapSlot = view.findViewById(R.id.tvDriverMapSlot);
        btnDriverMapAccept = view.findViewById(R.id.btnDriverMapAccept);
        btnDriverMapNavigate = view.findViewById(R.id.btnDriverMapNavigate);
        btnDriverMapReject = view.findViewById(R.id.btnDriverMapReject);

        rvMapGates.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnMainCampus.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(MAIN_CAMPUS_CENTER, 15.0f));
            }
        });

        btnFocusZone7.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ZONE_7_CENTER, 16.0f));
                Toast.makeText(requireContext(), "Showing Old Mukesh Gate (Zone 7)", Toast.LENGTH_SHORT).show();
            }
        });

        checkUserRole();
        initMap();
        loadData();
    }

    private void checkUserRole() {
        FirebaseUser user = AuthRepository.getInstance().getFirebaseUser();
        if (user != null) {
            FirestoreRepository.getInstance().checkIsDriver(user.getUid(), new FirestoreRepository.FirestoreCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isDriver) {
                    if (isAdded()) {
                        isDriverUser = isDriver != null && isDriver;
                        applyDriverUiMode();
                    }
                }

                @Override
                public void onError(Exception e) {
                }
            });
        }
    }

    private void applyDriverUiMode() {
        if (isDriverUser) {
            if (tvMapGatesHeader != null) tvMapGatesHeader.setVisibility(View.GONE);
            if (rvMapGates != null) rvMapGates.setVisibility(View.GONE);

            if (mapCard != null) {
                ViewGroup.LayoutParams params = mapCard.getLayoutParams();
                if (params instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
                    linearParams.height = 0;
                    linearParams.weight = 1.0f;
                    mapCard.setLayoutParams(linearParams);
                }
            }

            startDriverPendingListener();
        }
    }

    private void startDriverPendingListener() {
        FirebaseUser user = AuthRepository.getInstance().getFirebaseUser();
        if (user == null) return;

        if (driverPendingListener != null) {
            driverPendingListener.remove();
        }

        driverPendingListener = FirestoreRepository.getInstance().listenToPendingRideRequests(user.getUid(), new FirestoreRepository.FirestoreCallback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                if (isAdded()) {
                    driverPendingRequests.clear();
                    if (requests != null) {
                        driverPendingRequests.addAll(requests);
                    }
                    if (!driverPendingRequests.isEmpty()) {
                        currentDriverSelectedRequest = driverPendingRequests.get(0);
                    } else {
                        currentDriverSelectedRequest = null;
                    }
                    renderDriverMapAndAction();
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void renderDriverMapAndAction() {
        if (!isDriverUser || googleMap == null) return;

        if (currentDriverSelectedRequest == null) {
            if (cardDriverAction != null) cardDriverAction.setVisibility(View.GONE);
            renderAllMarkers();
            return;
        }

        RideRequest req = currentDriverSelectedRequest;
        if (cardDriverAction != null) {
            cardDriverAction.setVisibility(View.VISIBLE);
            tvDriverMapRoute.setText(req.getPickupGate() + " → " + req.getDestinationHub());
            tvDriverMapSlot.setText("Departure Slot: " + req.getDepartureSlot());

            FirebaseUser user = AuthRepository.getInstance().getFirebaseUser();
            String driverId = user != null ? user.getUid() : "";

            if (btnDriverMapNavigate != null) {
                btnDriverMapNavigate.setOnClickListener(v -> DriverRideAdapter.openGoogleMapsDirections(requireContext(), req.getPickupGate(), req.getDestinationHub()));
            }

            btnDriverMapAccept.setOnClickListener(v -> {
                btnDriverMapAccept.setEnabled(false);
                FirestoreRepository.getInstance().acceptRideRequestTransaction(req.getRequestId(), driverId, new FirestoreRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (isAdded()) {
                            btnDriverMapAccept.setEnabled(true);
                            Toast.makeText(requireContext(), "Ride accepted successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            btnDriverMapAccept.setEnabled(true);
                            String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to accept ride";
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });

            btnDriverMapReject.setOnClickListener(v -> {
                btnDriverMapReject.setEnabled(false);
                FirestoreRepository.getInstance().rejectRideRequest(req.getRequestId(), driverId, new FirestoreRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (isAdded()) {
                            btnDriverMapReject.setEnabled(true);
                            Toast.makeText(requireContext(), "Request hidden", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            btnDriverMapReject.setEnabled(true);
                            String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Failed to reject request";
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }

        // Draw Map Pickup & Destination Markers and Route Polyline
        googleMap.clear();

        CampusZoneConfig.ZoneInfo pickupZone = CampusZoneConfig.findZoneByIdOrName(req.getPickupGate());
        CampusZoneConfig.ZoneInfo destZone = CampusZoneConfig.findZoneByIdOrName(req.getDestinationHub());

        LatLng pickupPos = pickupZone != null ? new LatLng(pickupZone.getLatitude(), pickupZone.getLongitude()) : null;
        LatLng destPos = destZone != null ? new LatLng(destZone.getLatitude(), destZone.getLongitude()) : null;

        if (pickupPos != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(pickupPos)
                    .title("Pickup: " + req.getPickupGate())
                    .snippet("Departure Slot: " + req.getDepartureSlot())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (destPos != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(destPos)
                    .title("Destination: " + req.getDestinationHub())
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

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapContainer);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        if (pbMapLoading != null) {
            pbMapLoading.setVisibility(View.GONE);
        }

        try {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAIN_CAMPUS_CENTER, 14.8f));

            googleMap.setOnInfoWindowClickListener(marker -> {
                if (isDriverUser) return;
                String title = marker.getTitle();
                if (title != null) {
                    CampusZoneConfig.ZoneInfo zone = CampusZoneConfig.findZoneByIdOrName(title);
                    if (zone != null) {
                        if (zone.isPickup()) {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).navigateToRideShare(zone.getId(), null, null);
                            }
                        } else {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).navigateToRideShare(null, zone.getId(), null);
                            }
                        }
                    } else if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToRideShare(title, null, null);
                    }
                }
            });

            if (isDriverUser) {
                renderDriverMapAndAction();
            } else {
                renderAllMarkers();
            }
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Map initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadData() {
        FirestoreRepository repo = FirestoreRepository.getInstance();

        repo.getCampusGateDemands(new FirestoreRepository.FirestoreCallback<List<GateDemand>>() {
            @Override
            public void onSuccess(List<GateDemand> gates) {
                if (isAdded()) {
                    loadedGates = gates;
                    if (!isDriverUser) {
                        GateDemandAdapter adapter = new GateDemandAdapter(gates, gate -> {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).navigateToRideShare(gate.getGateId(), null, null);
                            }
                        });
                        rvMapGates.setAdapter(adapter);
                        renderAllMarkers();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });

        repo.getTransitHubs(new FirestoreRepository.FirestoreCallback<List<FirestoreRepository.TransitHub>>() {
            @Override
            public void onSuccess(List<FirestoreRepository.TransitHub> hubs) {
                if (isAdded()) {
                    loadedHubs = hubs;
                    if (!isDriverUser) {
                        renderAllMarkers();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void renderAllMarkers() {
        if (googleMap == null || isDriverUser) return;

        googleMap.clear();

        if (loadedGates != null) {
            for (GateDemand gate : loadedGates) {
                if (gate.getLatitude() != 0 && gate.getLongitude() != 0) {
                    LatLng pos = new LatLng(gate.getLatitude(), gate.getLongitude());
                    googleMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(gate.getGateId() + " - " + gate.getGateName())
                            .snippet("Pickup Zone • Tap to select for ride")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }
            }
        }

        if (loadedHubs != null) {
            for (FirestoreRepository.TransitHub hub : loadedHubs) {
                LatLng pos = new LatLng(hub.getLatitude(), hub.getLongitude());
                googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(hub.getId() + " - " + hub.getName())
                        .snippet("Destination Hub • Tap to select")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (driverPendingListener != null) {
            driverPendingListener.remove();
            driverPendingListener = null;
        }
    }
}