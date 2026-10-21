package com.example.campusride;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.campusride.adapter.GateDemandAdapter;
import com.example.campusride.data.AuthRepository;
import com.example.campusride.data.MamdaniApiClient;
import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.GateDemand;
import com.example.campusride.model.UserProfile;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvGreeting;
    private TextView tvPredictionTimeLabel;
    private TextView tvPredictionNote;
    private MaterialButton btnSelectTime;
    private MaterialButton btnLiveMode;
    private RecyclerView rvGateDemands;

    private boolean isLiveMode = true;
    private int selectedHour = -1;
    private int selectedMinute = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvPredictionTimeLabel = view.findViewById(R.id.tvPredictionTimeLabel);
        tvPredictionNote = view.findViewById(R.id.tvPredictionNote);
        btnSelectTime = view.findViewById(R.id.btnSelectTime);
        btnLiveMode = view.findViewById(R.id.btnLiveMode);
        rvGateDemands = view.findViewById(R.id.rvGateDemands);

        rvGateDemands.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnLiveMode.setOnClickListener(v -> {
            isLiveMode = true;
            updatePredictions();
            Toast.makeText(requireContext(), "Returned to Live Mode", Toast.LENGTH_SHORT).show();
        });

        updateGreeting();
        updatePredictions();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
        if (isLiveMode) {
            updatePredictions();
        }
    }

    private void updateGreeting() {
        if (tvGreeting == null) return;

        UserProfile user = AuthRepository.getInstance().getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            String name = user.getDisplayName().trim();
            String[] parts = name.split("\\s+");
            if (parts.length > 0 && !TextUtils.isEmpty(parts[0])) {
                tvGreeting.setText("Hi, " + parts[0] + " 👋");
                return;
            }
        }
        tvGreeting.setText("Hi there 👋");
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        int hour = selectedHour >= 0 ? selectedHour : cal.get(Calendar.HOUR_OF_DAY);
        int minute = selectedMinute >= 0 ? selectedMinute : cal.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
            selectedHour = hourOfDay;
            selectedMinute = minuteOfHour;
            isLiveMode = false;
            updatePredictions();
        }, hour, minute, false);

        dialog.show();
    }

    private void updatePredictions() {
        Calendar cal = Calendar.getInstance();
        if (!isLiveMode && selectedHour >= 0) {
            cal.set(Calendar.HOUR_OF_DAY, selectedHour);
            cal.set(Calendar.MINUTE, selectedMinute);
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        String dayName = dayFormat.format(cal.getTime());
        String formattedTime = timeFormat.format(cal.getTime());

        if (isLiveMode) {
            tvPredictionTimeLabel.setText("Mode: Live Mode • " + dayName + ", " + formattedTime);
            tvPredictionNote.setText("Demand predictions active for current timetable window.");
            btnLiveMode.setVisibility(View.GONE);
        } else {
            tvPredictionTimeLabel.setText("Mode: Test Selection • " + dayName + ", " + formattedTime);
            tvPredictionNote.setText("Test prediction calculated for " + formattedTime + ". Tap Live Mode to reset.");
            btnLiveMode.setVisibility(View.VISIBLE);
        }

        calculateZonePredictions(cal);
    }

    private void calculateZonePredictions(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        List<GateDemand> gates = new ArrayList<>();
        List<CampusZoneConfig.ZoneInfo> zones = CampusZoneConfig.getPickupZones();

        for (CampusZoneConfig.ZoneInfo z : zones) {
            double outflow;
            double dismissalMins;

            if (hour >= 11 && hour <= 13) {
                if ("Z3".equals(z.getId())) outflow = 220;
                else if ("Z4".equals(z.getId())) outflow = 160;
                else if ("Z6".equals(z.getId())) outflow = 140;
                else outflow = 70;
                dismissalMins = Math.max(3, (15 - (minute % 15)));
            } else if (hour >= 15 && hour <= 18) {
                if ("Z3".equals(z.getId())) outflow = 240;
                else if ("Z6".equals(z.getId())) outflow = 180;
                else if ("Z1".equals(z.getId())) outflow = 120;
                else outflow = 80;
                dismissalMins = Math.max(2, (10 - (minute % 10)));
            } else {
                outflow = 40;
                dismissalMins = 35;
            }

            GateDemand eval = MamdaniApiClient.getInstance().evaluateZonePriority(
                    z.getId(),
                    z.getName(),
                    z.getLatitude(),
                    z.getLongitude(),
                    outflow,
                    dismissalMins,
                    null
            );

            gates.add(eval);
        }

        GateDemandAdapter adapter = new GateDemandAdapter(gates, gate -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToRideShare(gate.getGateId(), null, null);
            }
        });
        rvGateDemands.setAdapter(adapter);
    }
}