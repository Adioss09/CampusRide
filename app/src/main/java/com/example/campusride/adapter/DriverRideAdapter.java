package com.example.campusride.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.R;
import com.example.campusride.model.CampusZoneConfig;
import com.example.campusride.model.RideRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DriverRideAdapter extends RecyclerView.Adapter<DriverRideAdapter.ViewHolder> {

    public interface OnActionListener {
        void onAcceptClick(RideRequest request);
        void onRejectClick(RideRequest request);
    }

    private final List<RideRequest> requests;
    private final OnActionListener listener;

    public DriverRideAdapter(List<RideRequest> requests, OnActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest item = requests.get(position);

        holder.tvRoute.setText(item.getPickupGate() + " → " + item.getDestinationHub());
        holder.tvSlot.setText("Departure Slot: " + item.getDepartureSlot());

        String studentName = item.getUserName() != null ? item.getUserName() : "Student";
        String shareStr = item.isWillingToShare() ? "Willing to share ride" : "Private ride request";
        holder.tvStudentInfo.setText("Student: " + studentName + " • " + shareStr);

        if (item.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM", Locale.US);
            holder.tvTime.setText("Requested at " + sdf.format(new Date(item.getTimestamp())));
        } else {
            holder.tvTime.setText("");
        }

        String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "PENDING";
        holder.tvStatusBadge.setText(status);

        if (holder.btnNavigateMaps != null) {
            holder.btnNavigateMaps.setOnClickListener(v -> openGoogleMapsDirections(holder.itemView.getContext(), item.getPickupGate(), item.getDestinationHub()));
        }

        if ("MATCHED".equals(status) || "CONFIRMED".equals(status) || "COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            holder.layoutActions.setVisibility(View.GONE);
            if ("MATCHED".equals(status) || "CONFIRMED".equals(status)) {
                holder.tvStatusBadge.setBackgroundTintList(holder.itemView.getContext().getColorStateList(R.color.demand_low_bg));
                holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.demand_low_text));
                holder.tvStatusBadge.setText("ACCEPTED");
            }
        } else {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnAcceptRide.setEnabled(true);
            holder.btnRejectRide.setEnabled(true);
            holder.pbActionLoading.setVisibility(View.GONE);

            holder.btnAcceptRide.setOnClickListener(v -> {
                if (listener != null) {
                    holder.btnAcceptRide.setEnabled(false);
                    holder.btnRejectRide.setEnabled(false);
                    holder.pbActionLoading.setVisibility(View.VISIBLE);
                    listener.onAcceptClick(item);
                }
            });

            holder.btnRejectRide.setOnClickListener(v -> {
                if (listener != null) {
                    holder.btnAcceptRide.setEnabled(false);
                    holder.btnRejectRide.setEnabled(false);
                    holder.pbActionLoading.setVisibility(View.VISIBLE);
                    listener.onRejectClick(item);
                }
            });
        }
    }

    public static void openGoogleMapsDirections(Context context, String pickupGate, String destinationHub) {
        CampusZoneConfig.ZoneInfo pickupZone = CampusZoneConfig.findZoneByIdOrName(pickupGate);
        CampusZoneConfig.ZoneInfo destZone = CampusZoneConfig.findZoneByIdOrName(destinationHub);

        if (pickupZone == null || destZone == null) {
            Toast.makeText(context, "Location coordinates unavailable for directions.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uriStr = String.format(Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                pickupZone.getLatitude(), pickupZone.getLongitude(),
                destZone.getLatitude(), destZone.getLongitude());

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
        intent.setPackage("com.google.android.apps.maps");
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            context.startActivity(browserIntent);
        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvStatusBadge, tvSlot, tvStudentInfo, tvTime;
        LinearLayout layoutActions;
        MaterialButton btnAcceptRide, btnNavigateMaps, btnRejectRide;
        ProgressBar pbActionLoading;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvSlot = itemView.findViewById(R.id.tvSlot);
            tvStudentInfo = itemView.findViewById(R.id.tvStudentInfo);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnAcceptRide = itemView.findViewById(R.id.btnAcceptRide);
            btnNavigateMaps = itemView.findViewById(R.id.btnNavigateMaps);
            btnRejectRide = itemView.findViewById(R.id.btnRejectRide);
            pbActionLoading = itemView.findViewById(R.id.pbActionLoading);
        }
    }
}
