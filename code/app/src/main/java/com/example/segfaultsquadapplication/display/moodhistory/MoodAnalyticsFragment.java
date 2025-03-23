package com.example.segfaultsquadapplication.display.moodhistory;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.firebase.auth.FirebaseAuth;
import com.github.mikephil.charting.charts.PieChart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageButton;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.widget.FrameLayout;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class MoodAnalyticsFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private List<MoodEvent> moodEvents = new ArrayList<>();
    private PieChart moodDistributionChart;
    private HorizontalBarChart recentMoodsChart;
    private ChipGroup moodDistributionChipGroup;
    private ChipGroup recentMoodsChipGroup;
    private Chip myMoodsChip, communityMoodsChip;
    private Chip myRecentMoodsChip, communityRecentMoodsChip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood_analytics, container, false);

        // Hide bottom navigation
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.BottomNavBar); // Adjust the ID as necessary
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        }

        // Initialize views
        moodDistributionChart = view.findViewById(R.id.moodDistributionChart);
        recentMoodsChart = view.findViewById(R.id.recentMoodsChart);
        moodDistributionChipGroup = view.findViewById(R.id.moodDistributionChipGroup);
        recentMoodsChipGroup = view.findViewById(R.id.recentMoodsChipGroup);
        myMoodsChip = view.findViewById(R.id.myMoodsChip);
        communityMoodsChip = view.findViewById(R.id.communityMoodsChip);
        myRecentMoodsChip = view.findViewById(R.id.myRecentMoodsChip);
        communityRecentMoodsChip = view.findViewById(R.id.communityRecentMoodsChip);

        // Set up chip listeners
        setupChipListeners();

        // Load initial data
        loadAnalyticsData();

        // Set up back button with animation
        ImageButton buttonBack = view.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100);
                requireActivity().onBackPressed();
            }).start();
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Start emoji rain after view is created
        startEmojiRain();
    }

    /**
     * listener setup for the chips to toggle between personal and global mood event data
     */
    private void setupChipListeners() {
        myMoodsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                loadMoodDistribution(true);
            }
        });

        communityMoodsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                loadMoodDistribution(false);
            }
        });

        myRecentMoodsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                loadRecentMoods(true);
            }
        });

        communityRecentMoodsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                loadRecentMoods(false);
            }
        });
    }

    /**
     * method to load in analytics data (just calls sub methods to load in data and create graphs)
     */
    private void loadAnalyticsData() {
        // Load personal moods by default
        loadMoodDistribution(true);
        loadRecentMoods(true);
    }

    /**
     * method to load in mood events and create pie chart
     * @param personalOnly
     * bool to load in only current user's (true) or all (false) mood events
     */
    private void loadMoodDistribution(boolean personalOnly) {
        String userId = personalOnly ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        ArrayList<MoodEvent> holder = new ArrayList<>();

        MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, holder, isSuccess -> {
            if (isSuccess) {
                for (MoodEvent mood : holder) {
                    Log.d("MoodAnalytics", "Loaded mood: " + mood.getMoodType());
                    moodEvents.add(mood);
                }
                displayAnalytics(moodEvents);
            } else {
                Toast.makeText(getContext(), "Error loading mood distribution", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * method to load in recent mood events for bar chart
     * @param personalOnly
     * bool to load in only current user's (true) or all (false) mood events
     */
    private void loadRecentMoods(boolean personalOnly) {
        String userId = personalOnly ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        ArrayList<MoodEvent> holder = new ArrayList<>();

        MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, holder, isSuccess -> {
            if (isSuccess) {
                updateBarChart(holder);
            } else {
                Toast.makeText(getContext(), "Error loading recent moods", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * method to update the recent moods count bar chart
     * @param moodEvents
     * list of mood events to be displayed
     */
    private void updateBarChart(List<MoodEvent> moodEvents) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        Map<MoodEvent.MoodType, Integer> moodCounts = new HashMap<>();

        // Count occurrences of each mood type
        for (MoodEvent mood : moodEvents) {
            moodCounts.put(mood.getMoodType(),
                    moodCounts.getOrDefault(mood.getMoodType(), 0) + 1);
        }

        // Create entries for the bar chart
        int index = 0;
        ArrayList<String> labels = new ArrayList<>();
        for (MoodEvent.MoodType moodType : MoodEvent.MoodType.values()) {
            int count = moodCounts.getOrDefault(moodType, 0);
            entries.add(new BarEntry(index++, count));
            labels.add(moodType.name());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Mood Counts");
        dataSet.setColors(getMoodColors());

        BarData barData = new BarData(dataSet);
        recentMoodsChart.setData(barData);

        // Configure X-axis
        XAxis xAxis = recentMoodsChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setDrawGridLines(false);
        // Add left padding to prevent label cutoff, i dont know why its happening to be
        // honest
        xAxis.setSpaceMin(0.5f);

        // Configure Y-axis
        recentMoodsChart.getAxisLeft().setAxisMinimum(0f); // Start at 0
        recentMoodsChart.getAxisRight().setEnabled(false); // Disable right axis

        // Other chart settings
        recentMoodsChart.setDrawValueAboveBar(true);
        recentMoodsChart.setDescription(null);
        recentMoodsChart.setExtraLeftOffset(15f); // Add extra left margin
        recentMoodsChart.setExtraBottomOffset(10f); // Add extra bottom margin for labels

        // Animate chart
        recentMoodsChart.animateY(500);
        recentMoodsChart.invalidate();
    }

    private int[] getMoodColors() {
        // Return an array of colors corresponding to each mood type
        MoodEvent.MoodType[] moodTypes = MoodEvent.MoodType.values();
        int[] colors = new int[moodTypes.length];
        for (int i = 0; i < moodTypes.length; i++) {
            colors[i] = moodTypes[i].getPrimaryColor(requireContext());
        }
        return colors;
    }

    /**
     * just silly analytics (most frequent mood type. Maight remove later)
     *
     * @param moodEvents
     *                   list of this user's mood events
     */
    private void displayAnalytics(List<MoodEvent> moodEvents) {
        setupPieChart(moodEvents);
        fadeInViews();
    }

    /**
     * method to setup and activate the emoji rain animation effect
     */
    private void startEmojiRain() {
        final String[] emojis = MoodEvent.MoodType.getAllEmoticons();
        final FrameLayout emojiRainContainer = requireView().findViewById(R.id.emojiRainContainer);
        if (emojiRainContainer == null)
            return;

        // Wait for layout to be measured
        emojiRainContainer.post(() -> {
            // Get actual container dimensions
            final int containerWidth = emojiRainContainer.getWidth();
            final int containerHeight = emojiRainContainer.getHeight();

            // Ensure we have valid dimensions before proceeding
            if (containerWidth <= 0 || containerHeight <= 0)
                return;

            // Create 100 emojis
            for (int i = 0; i < 100; i++) {
                final TextView emojiView = new TextView(getContext());
                emojiView.setText(emojis[(int) (Math.random() * emojis.length)]);
                emojiView.setTextSize(32);

                // Position horizontally across the width
                float xPosition = (float) (Math.random() * containerWidth);
                emojiView.setTranslationX(xPosition);

                // Start from above the visible area
                emojiView.setTranslationY(-2000);

                // Add to container
                emojiRainContainer.addView(emojiView);

                // Apply rotation and scaling
                float rotation = (float) (Math.random() * 720) - 360;
                float scale = 0.6f + (float) Math.random() * 0.8f;
                emojiView.setScaleX(scale);
                emojiView.setScaleY(scale);
                emojiView.setRotation(rotation);

                // Calculate a distance that guarantees the emoji will be fully off-screen
                float endY = containerHeight + 300; // Extra buffer to ensure it's gone

                // Create the falling animation
                ObjectAnimator fallAnimation = ObjectAnimator.ofFloat(
                        emojiView,
                        "translationY",
                        -200, // Clearly off-screen at top
                        endY // Clearly off-screen at bottom
                );

                // Set animation parameters
                fallAnimation.setDuration(2000 + (long) (Math.random() * 3000));
                fallAnimation.setInterpolator(new AccelerateInterpolator(1.2f));

                // Remove view when animation is complete
                fallAnimation.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        emojiRainContainer.removeView(emojiView);
                    }
                });

                // Stagger start times
                fallAnimation.setStartDelay(i * 30);
                fallAnimation.start();
            }
        });
    }

    /**
     * Distribution of mood types
     *
     * @param moodEvents
     *                   list of all of this user's mood events
     */
    private void setupPieChart(List<MoodEvent> moodEvents) {
        Map<MoodEvent.MoodType, Integer> moodCountMap = new HashMap<>();
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Count occurrences of each mood type
        for (MoodEvent mood : moodEvents) {
            moodCountMap.put(mood.getMoodType(), moodCountMap.getOrDefault(mood.getMoodType(), 0) + 1);
        }

        // Create entries for the pie chart
        for (Map.Entry<MoodEvent.MoodType, Integer> entry : moodCountMap.entrySet()) {
            MoodEvent.MoodType moodType = entry.getKey();
            Integer count = entry.getValue();
            MoodEvent moodEvent = moodEvents.stream()
                    .filter(m -> m.getMoodType() == moodType)
                    .findFirst()
                    .orElse(null); // Get the first MoodEvent with this moodType

            if (moodEvent != null) {
                entries.add(new PieEntry(count, moodType.name()));
                colors.add(moodEvent.getMoodType().getPrimaryColor(requireContext())); // Get the corresponding color
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "Mood Distribution");
        dataSet.setColors(colors); // Set the custom colors
        PieData pieData = new PieData(dataSet);

        // Configure the pie chart
        moodDistributionChart.setUsePercentValues(true); // Show percentage values
        moodDistributionChart.setEntryLabelTextSize(12f);
        // moodDistributionChart.setDrawHole(true); // Create a hole in the middle
        moodDistributionChart.setHoleRadius(35f); // Size of the hole
        moodDistributionChart.setTransparentCircleRadius(40f); // Size of the transparent circle
        moodDistributionChart.setData(pieData);

        // Add animations
        moodDistributionChart.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad); // Vertical
        moodDistributionChart.spin(1000, 0, 360f, com.github.mikephil.charting.animation.Easing.EaseInOutQuad); // Spin

        moodDistributionChart.invalidate();
    }

    private void fadeInViews() {
        moodDistributionChart.animate().alpha(1f).setDuration(800).setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    public void onMoodClick(MoodEvent mood) {
        Toast.makeText(getContext(), "Clicked on mood: " + mood.getMoodType().name(), Toast.LENGTH_SHORT).show();
    }
}