package com.example.segfaultsquadapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MoodGridAdapter extends RecyclerView.Adapter<MoodGridAdapter.ViewHolder> {

    private List<MoodEvent> moodEvents;
    private Context context;

    public MoodGridAdapter(Context context, List<MoodEvent> moodEvents) {
        this.context = context;
        this.moodEvents = moodEvents;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mood_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoodEvent mood = moodEvents.get(position);
        holder.moodEmoji.setText(getEmojiString(mood.getMoodType())); // Set the emoji as text
        holder.itemView.setBackgroundColor(mood.getSecondaryColor(context)); // Use the context variable

        // debugging
        // Log to check if binding is happening correctly
        Log.d("MoodGridAdapter", "Binding mood at position " + position + ": " + mood.getMoodType()
                + getEmojiString(mood.getMoodType()));
    }

    @Override
    public int getItemCount() {
        return moodEvents.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView moodEmoji; // Change to TextView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            moodEmoji = itemView.findViewById(R.id.mood_emoji); // Ensure this is a TextView in your layout
        }
    }

    private String getEmojiString(MoodEvent.MoodType moodType) {
        switch (moodType) {
            case ANGER:
                return "😡";
            case CONFUSION:
                return "😵‍💫";
            case DISGUST:
                return "🤢";
            case FEAR:
                return "😨";
            case HAPPINESS:
                return "😀";
            case SADNESS:
                return "😭";
            case SHAME:
                return "😳";
            case SURPRISE:
                return "🤯";
            default:
                return "😶"; // Default emoji as string
        }
    }
}
