/**
 * Classname: MoodAdapter
 * Version Info: Initial
 * Date: Feb 18, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {
    // attributes
    private List<MoodEvent> moodList;
    private OnMoodClickListener listener;

    // interfaces
    public interface OnMoodClickListener {
        void onMoodClick(MoodEvent mood);
    }

    // methods
    // constructor(s)
    /**
     * constructor method
     *
     * @param listener
     *                 listerner to alert the adapter of updates
     */
    public MoodAdapter(OnMoodClickListener listener) {
        this.moodList = new ArrayList<>();
        this.listener = listener;
    }

    // parent class methods
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

    // new methods
    /**
     * method to update the moods in ArrayList
     *
     * @param newMoods
     */
    public void updateMoods(List<MoodEvent> newMoods) {
        this.moodList = newMoods;
        notifyDataSetChanged();
    }

    /**
     * nested class for View holding Moods
     */
    class MoodViewHolder extends RecyclerView.ViewHolder {
        // attributes
        private MaterialCardView moodCard;
        private TextView moodEmoji;
        private TextView textMoodType;
        private TextView textReason;
        private TextView textTimestamp;
        private TextView textSocialSituation;

        /**
         * constructor
         *
         * @param itemView
         *                 the view of the mood card
         */
        public MoodViewHolder(@NonNull View itemView) {
            super(itemView);
            moodCard = (MaterialCardView) itemView;
            moodEmoji = itemView.findViewById(R.id.moodEmoji);
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

        /**
         * method to create the UI of the mood event
         *
         * @param mood
         *             mood information object (MoodEvent)
         */
        public void bind(MoodEvent mood) {
            // Set mood emoji
            moodEmoji.setText(mood.getMoodType().getEmoticon());

            textMoodType.setText(mood.getMoodType().name());
            textReason.setText(mood.getReasonText());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            textTimestamp.setText(sdf.format(mood.getTimestampDate()));

            if (mood.getSocialSituation() != null) {
                textSocialSituation.setText(mood.getSocialSituation().toString());
                textSocialSituation.setVisibility(View.VISIBLE);
            } else {
                textSocialSituation.setVisibility(View.GONE);
            }

            // Set mood colors
            int moodColor = mood.getMoodType().getPrimaryColor(itemView.getContext());
            int backgroundColor = mood.getMoodType().getSecondaryColor(itemView.getContext());

            moodCard.setStrokeColor(moodColor);
            moodCard.setCardBackgroundColor(backgroundColor);
        }
    }
}