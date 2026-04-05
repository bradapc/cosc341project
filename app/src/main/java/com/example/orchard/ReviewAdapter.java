package com.example.orchard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<WorkerShiftSummary> workerShifts;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WorkerShiftSummary summary);
    }

    public ReviewAdapter(List<WorkerShiftSummary> workerShifts, OnItemClickListener listener) {
        this.workerShifts = workerShifts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkerShiftSummary summary = workerShifts.get(position);
        holder.workerNameText.setText(summary.getWorkerName());
        holder.shiftDurationText.setText("Duration: " + summary.getDuration());
        holder.binCountText.setText(summary.getTotalBins() + " Bins");
        
        if (summary.isApproved()) {
            holder.statusBadge.setText("Approved");
            holder.statusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.primary_green));
        } else {
            holder.statusBadge.setText("Pending");
            holder.statusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.accent_orange));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(summary));
    }

    @Override
    public int getItemCount() {
        return workerShifts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView workerNameText, shiftDurationText, binCountText, statusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            workerNameText = itemView.findViewById(R.id.workerNameText);
            shiftDurationText = itemView.findViewById(R.id.shiftDurationText);
            binCountText = itemView.findViewById(R.id.binCountText);
            statusBadge = itemView.findViewById(R.id.statusBadge);
        }
    }

    public static class WorkerShiftSummary {
        private String workerId;
        private String shiftId;
        private String workerName;
        private String duration;
        private int totalBins;
        private boolean approved;

        public WorkerShiftSummary(String workerId, String shiftId, String workerName, String duration, int totalBins, boolean approved) {
            this.workerId = workerId;
            this.shiftId = shiftId;
            this.workerName = workerName;
            this.duration = duration;
            this.totalBins = totalBins;
            this.approved = approved;
        }

        public String getWorkerId() { return workerId; }
        public String getShiftId() { return shiftId; }
        public String getWorkerName() { return workerName; }
        public String getDuration() { return duration; }
        public int getTotalBins() { return totalBins; }
        public boolean isApproved() { return approved; }
    }
}
