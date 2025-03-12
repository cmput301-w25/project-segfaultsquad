/**
 * Classname: RecentActivityAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.recentactivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<MoodEvent> recentActivities;

    public RecentActivityAdapter(List<MoodEvent> recentActivities) {
        this.recentActivities = recentActivities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoodEvent mood = recentActivities.get(position);
        holder.activityText.setText("Posted a new mood: " + mood.getMoodType()); // Customize as needed
    }

    @Override
    public int getItemCount() {
        return recentActivities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            activityText = itemView.findViewById(R.id.activity_text);
        }
    }
}
