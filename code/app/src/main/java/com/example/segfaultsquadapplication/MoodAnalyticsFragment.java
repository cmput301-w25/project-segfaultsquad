package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageButton;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import android.graphics.Color;
import com.github.mikephil.charting.data.Entry;
import android.widget.FrameLayout;

public class MoodAnalyticsFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private TextView mostCommonMoodText;
    private TextView averageMoodText;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> moodEvents = new ArrayList<>();
    private PieChart moodDistributionChart;
    private LineChart moodTrendChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood_analytics, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        mostCommonMoodText = view.findViewById(R.id.mostCommonMoodText);
        averageMoodText = view.findViewById(R.id.averageMoodText);
        moodDistributionChart = view.findViewById(R.id.moodDistributionChart);
        moodTrendChart = view.findViewById(R.id.moodTrendChart);

        // Load and display analytics data
        loadAnalyticsData();

        // Set up back button
        ImageButton buttonBack = view.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            requireActivity().onBackPressed(); // Navigate back to MyMoodHistoryFragment
        });

        return view;
    }

    private void loadAnalyticsData() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MoodEvent mood = doc.toObject(MoodEvent.class);
                        moodEvents.add(mood);
                    }
                    displayAnalytics(moodEvents);
                    // Start emoji rain animation after data is loaded
                    startEmojiRain();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading mood data", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayAnalytics(List<MoodEvent> moodEvents) {
        // Calculate mood distribution
        Map<MoodEvent.MoodType, Integer> moodCountMap = new HashMap<>();
        for (MoodEvent mood : moodEvents) {
            moodCountMap.put(mood.getMoodType(), moodCountMap.getOrDefault(mood.getMoodType(), 0) + 1);
        }

        // Find the most common mood
        MoodEvent.MoodType mostCommonMood = null;
        int maxCount = 0;
        for (Map.Entry<MoodEvent.MoodType, Integer> entry : moodCountMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommonMood = entry.getKey();
            }
        }

        // Calculate average mood
        int totalMoodValue = 0;
        for (MoodEvent mood : moodEvents) {
            totalMoodValue += mood.getMoodType().ordinal() + 1; // Assuming ordinal gives a value from 1 to 8
        }
        double averageMood = moodEvents.size() > 0 ? (double) totalMoodValue / moodEvents.size() : 0;

        // Update UI
        mostCommonMoodText.setText("Most Common Mood: " + (mostCommonMood != null ? mostCommonMood.name() : "None"));
        averageMoodText.setText("Average Mood: " + String.format("%.2f", averageMood));

        // Set up PieChart
        setupPieChart(moodCountMap);

        // Set up LineChart
        setupLineChart(moodEvents);
    }

    private void setupPieChart(Map<MoodEvent.MoodType, Integer> moodCountMap) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (Map.Entry<MoodEvent.MoodType, Integer> entry : moodCountMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey().name()));
            colors.add(getMoodColor(entry.getKey())); // Get the corresponding color
        }

        PieDataSet dataSet = new PieDataSet(entries, "Mood Distribution");
        dataSet.setColors(colors); // Set the custom colors
        PieData pieData = new PieData(dataSet);
        moodDistributionChart.setData(pieData);
        moodDistributionChart.invalidate(); // refresh
    }

    private int getMoodColor(MoodEvent.MoodType moodType) {
        switch (moodType) {
            case HAPPY:
                return getResources().getColor(R.color.mood_happy);
            case SAD:
                return getResources().getColor(R.color.mood_sad);
            case ANGRY:
                return getResources().getColor(R.color.mood_angry);
            case EXCITED:
                return getResources().getColor(R.color.mood_excited);
            case TIRED:
                return getResources().getColor(R.color.mood_tired);
            case SCARED:
                return getResources().getColor(R.color.mood_scared);
            case SURPRISED:
                return getResources().getColor(R.color.mood_surprised);
            default:
                return getResources().getColor(R.color.mood_default);
        }
    }

    private void setupLineChart(List<MoodEvent> moodEvents) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < moodEvents.size(); i++) {
            MoodEvent mood = moodEvents.get(i);
            entries.add(new Entry(i, mood.getMoodType().ordinal() + 1)); // Assuming ordinal gives a value from 1 to 8
        }

        LineDataSet dataSet = new LineDataSet(entries, "Mood Trend");
        dataSet.setColor(Color.BLUE); // Set your desired color
        dataSet.setValueTextColor(Color.BLACK); // Set value text color
        dataSet.setLineWidth(2f); // Set line width
        dataSet.setCircleColor(Color.RED); // Set circle color
        dataSet.setCircleRadius(4f); // Set circle radius
        dataSet.setDrawCircleHole(true); // Draw circle hole
        dataSet.setDrawValues(true); // Show values on the line

        LineData lineData = new LineData(dataSet);
        moodTrendChart.setData(lineData);
        moodTrendChart.getDescription().setEnabled(false); // Disable description
        moodTrendChart.getLegend().setEnabled(false); // Disable legend
        moodTrendChart.invalidate(); // refresh
    }

    private void startEmojiRain() {
        final String[] emojis = {
                "😡", // ANGRY
                "😭", // SAD
                "😀", // HAPPY
                "😆", // EXCITED
                "😴", // TIRED
                "😱", // SCARED
                "🤯" // SURPRISED
        };

        // Ensure you are using the correct view reference
        FrameLayout emojiRainContainer = getView().findViewById(R.id.emojiRainContainer);
        if (emojiRainContainer == null) {
            Toast.makeText(getContext(), "Emoji rain container not found!", Toast.LENGTH_SHORT).show();
            return; // Exit if the container is not found
        }

        for (int i = 0; i < 60; i++) { // Number of emojis to rain
            final TextView emojiView = new TextView(getContext());
            emojiView.setText(emojis[(int) (Math.random() * emojis.length)]); // Directly set the emoji string
            emojiView.setTextSize(32);
            emojiView.setTranslationX((float) (Math.random() * emojiRainContainer.getWidth() - 60));
            emojiView.setTranslationY(-100); // Start above the screen
            emojiRainContainer.addView(emojiView);

            // Animate falling
            emojiView.animate()
                    .translationY(emojiRainContainer.getHeight() + 100) // Fall to the bottom
                    .setDuration(3000 + (long) (Math.random() * 2000)) // Random duration
                    .withEndAction(() -> emojiRainContainer.removeView(emojiView)) // Remove after animation
                    .start();
        }
    }

    @Override
    public void onMoodClick(MoodEvent mood) {
        // Handle mood click event
        Toast.makeText(getContext(), "Clicked on mood: " + mood.getMoodType().name(), Toast.LENGTH_SHORT).show();
    }
}