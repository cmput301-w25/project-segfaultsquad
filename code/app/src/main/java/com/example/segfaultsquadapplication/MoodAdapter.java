package com.example.segfaultsquadapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {
    private List<MoodEvent> moodList;
    private OnMoodClickListener listener;

    public interface OnMoodClickListener {
        void onMoodClick(MoodEvent mood);
    }

    public MoodAdapter(OnMoodClickListener listener) {
        this.moodList = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood, parent, false);
        return new MoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodViewHolder holder, int position) {
        MoodEvent mood = moodList.get(position);
        holder.bind(mood);
    }

    @Override
    public int getItemCount() {
        return moodList.size();
    }

    public void updateMoods(List<MoodEvent> newMoods) {
        this.moodList = newMoods;
        notifyDataSetChanged();
    }

    class MoodViewHolder extends RecyclerView.ViewHolder {
        private View moodIndicator;
        private TextView textMoodType;
        private TextView textReason;
        private TextView textTimestamp;
        private TextView textSocialSituation;

        public MoodViewHolder(@NonNull View itemView) {
            super(itemView);
            moodIndicator = itemView.findViewById(R.id.moodIndicator);
            textMoodType = itemView.findViewById(R.id.textMoodType);
            textReason = itemView.findViewById(R.id.textReason);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textSocialSituation = itemView.findViewById(R.id.textSocialSituation);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMoodClick(moodList.get(position));
                }
            });
        }

        public void bind(MoodEvent mood) {
            textMoodType.setText(mood.getMoodType().name());
            textReason.setText(mood.getReasonText());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            textTimestamp.setText(sdf.format(mood.getTimestampDate()));

            if (mood.getSocialSituation() != null) {
                textSocialSituation.setText(mood.getSocialSituation().name());
                textSocialSituation.setVisibility(View.VISIBLE);
            } else {
                textSocialSituation.setVisibility(View.GONE);
            }

            // Set mood indicator color based on mood type
            moodIndicator.setBackgroundColor(getMoodColor(mood.getMoodType()));
        }

        private int getMoodColor(MoodEvent.MoodType moodType) {
            switch (moodType) {
                case HAPPY:
                    return itemView.getContext().getColor(R.color.mood_happy);
                case SAD:
                    return itemView.getContext().getColor(R.color.mood_sad);
                case ANGRY:
                    return itemView.getContext().getColor(R.color.mood_angry);
                case EXCITED:
                    return itemView.getContext().getColor(R.color.mood_excited);
                case TIRED:
                    return itemView.getContext().getColor(R.color.mood_tired);
                case SCARED:
                    return itemView.getContext().getColor(R.color.mood_scared);
                case SURPRISED:
                    return itemView.getContext().getColor(R.color.mood_surprised);
                default:
                    return itemView.getContext().getColor(R.color.mood_default);
            }
        }
    }
}