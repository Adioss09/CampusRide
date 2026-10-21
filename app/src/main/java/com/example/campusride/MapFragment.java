package com.example.campusride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.adapter.GateDemandAdapter;
import com.example.campusride.data.FirestoreRepository;
import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.GateDemand;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private RecyclerView rvMapGates;
    private ProgressBar pbMapLoading;
    private MaterialButton btnMainCampus;
    private MaterialButton btnFocusZone7;
    private GoogleMap googleMap;
    private List<GateDemand> loadedGates;
    private List<FirestoreRepository.TransitHub> loadedHubs;

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
        pbMapLoading = view.findViewById(R.id.pbMapLoading);
        btnMainCampus = view.findViewById(R.id.btnMainCampus);
        btnFocusZone7 = view.findViewById(R.id.btnFocusZone7);

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

        initMap();
        loadData();
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

            // Initial camera centered on Main Campus cluster
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAIN_CAMPUS_CENTER, 14.8f));

            // Set info window click listener to trigger quick ride request
            googleMap.setOnInfoWindowClickListener(marker -> {
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

            renderAllMarkers();
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
                    GateDemandAdapter adapter = new GateDemandAdapter(gates, gate -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToRideShare(gate.getGateId(), null, null);
                        }
                    });
                    rvMapGates.setAdapter(adapter);

                    renderAllMarkers();
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
                    renderAllMarkers();
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void renderAllMarkers() {
        if (googleMap == null) return;

        // Clear existing markers to prevent duplicates
        googleMap.clear();

        // 1. Add Pickup Gate Markers (Z1 - Z7)
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

        // 2. Add External Transit Hub Markers (H1 - H4)
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
}