package com.example.campusride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.R;
import com.example.campusride.model.RideRequest;

import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClick(RideRequest request);
    }

    private final List<RideRequest> requests;
    private final OnCancelClickListener cancelListener;

    public RideRequestAdapter(List<RideRequest> requests, OnCancelClickListener cancelListener) {
        this.requests = requests;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest item = requests.get(position);

        holder.tvStatusBadge.setText(item.getStatus());
        holder.tvSlot.setText("Departure: " + item.getDepartureSlot());
        holder.tvRoute.setText(item.getPickupGate() + " → " + item.getDestinationHub());
        holder.tvShareInfo.setText(item.isWillingToShare() ? "Willing to share rides" : "Private ride request");

        if ("CANCELLED".equals(item.getStatus())) {
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancelClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatusBadge, tvSlot, tvRoute, tvShareInfo;
        Button btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvSlot = itemView.findViewById(R.id.tvSlot);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvShareInfo = itemView.findViewById(R.id.tvShareInfo);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}