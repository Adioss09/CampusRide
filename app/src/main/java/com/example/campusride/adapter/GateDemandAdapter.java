package com.example.campusride.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusride.R;
import com.example.campusride.model.GateDemand;

import java.util.List;
import java.util.Locale;

public class GateDemandAdapter extends RecyclerView.Adapter<GateDemandAdapter.ViewHolder> {

    public interface OnGateClickListener {
        void onGateClick(GateDemand gate);
    }

    private final List<GateDemand> gates;
    private final OnGateClickListener clickListener;

    public GateDemandAdapter(List<GateDemand> gates) {
        this(gates, null);
    }

    public GateDemandAdapter(List<GateDemand> gates, OnGateClickListener clickListener) {
        this.gates = gates;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gate_demand, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GateDemand item = gates.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvGateName.setText(item.getGateName());
        holder.tvEstOutflow.setText(String.format(Locale.US, "%d students", item.getEstimatedOutflow()));
        holder.tvTimeWindow.setText(String.format(Locale.US, "%d mins remaining", item.getTimeUntilDismissal()));
        holder.tvPriorityScore.setText(String.format(Locale.US, "%.2f / 1.00", item.getPriorityScore()));
        holder.tvExplanation.setText(item.getExplanation());

        String label = item.getPriorityLabel() != null ? item.getPriorityLabel() : "LOW";
        holder.tvDemandBadge.setText(label);

        switch (label) {
            case "CRITICAL":
                holder.tvDemandBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, R.color.demand_critical_bg));
                holder.tvDemandBadge.setTextColor(ContextCompat.getColor(ctx, R.color.demand_critical_text));
                break;
            case "HIGH":
                holder.tvDemandBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, R.color.demand_high_bg));
                holder.tvDemandBadge.setTextColor(ContextCompat.getColor(ctx, R.color.demand_high_text));
                break;
            case "MEDIUM":
                holder.tvDemandBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, R.color.demand_medium_bg));
                holder.tvDemandBadge.setTextColor(ContextCompat.getColor(ctx, R.color.demand_medium_text));
                break;
            case "LOW":
            default:
                holder.tvDemandBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, R.color.demand_low_bg));
                holder.tvDemandBadge.setTextColor(ContextCompat.getColor(ctx, R.color.demand_low_text));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onGateClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gates != null ? gates.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGateName, tvDemandBadge, tvExplanation, tvEstOutflow, tvTimeWindow, tvPriorityScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGateName = itemView.findViewById(R.id.tvGateName);
            tvDemandBadge = itemView.findViewById(R.id.tvDemandBadge);
            tvExplanation = itemView.findViewById(R.id.tvExplanation);
            tvEstOutflow = itemView.findViewById(R.id.tvEstOutflow);
            tvTimeWindow = itemView.findViewById(R.id.tvTimeWindow);
            tvPriorityScore = itemView.findViewById(R.id.tvPriorityScore);
        }
    }
}