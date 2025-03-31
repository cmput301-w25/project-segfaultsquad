package com.example.segfaultsquadapplication.display.moodhistory;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mood Analytics fragment that displays mood statistics and visualizations.
 * This fragment provides visual representations of mood data through pie charts and bar charts,
 * allowing users to view their personal mood distributions and compare with community data.
 * It also includes interactive elements like filter chips and an animated emoji rain effect.
 */
public class MoodAnalyticsFragment extends Fragment {
    // Charts and UI elements
    private List<MoodEvent> moodEvents = new ArrayList<>();
    private PieChart moodDistributionChart;
    private HorizontalBarChart recentMoodsChart;
    private ChipGroup moodDistributionChipGroup;
    private ChipGroup recentMoodsChipGroup;
    private Chip myMoodsChip, communityMoodsChip;
    private Chip myRecentMoodsChip, communityRecentMoodsChip;

    /**
     * Inflates the fragment layout and initializes UI components.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState Previously saved state of the fragment
     * @return The root View of the fragment's layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood_analytics, container, false);

        // Hide bottom navigation
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.BottomNavBar);
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

    /**
     * Called after the view is created. Starts the emoji rain animation.
     *
     * @param view The created view
     * @param savedInstanceState Previously saved state of the fragment
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Start emoji rain after view is created
        startEmojiRain();
    }

    /**
     * Sets up listeners for the filter chips to toggle between personal and global mood data.
     * Each chip triggers a data reload based on the selected filter.
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
     * Loads analytics data and prepares visualizations.
     * This method serves as an entry point for data loading and visualization initialization.
     */
    private void loadAnalyticsData() {
        // Load personal moods by default
        loadMoodDistribution(true);
        loadRecentMoods(true);
    }

    /**
     * Loads mood distribution data for visualization in the pie chart.
     *
     * @param personalOnly If true, loads only the current user's mood events;
     *                    if false, loads all users' mood events
     */
    private void loadMoodDistribution(boolean personalOnly) {
        String userId = personalOnly ? UserManager.getUserId() : null;
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
     * Loads recent mood data for visualization in the bar chart.
     *
     * @param personalOnly If true, loads only the current user's mood events;
     *                    if false, loads all users' mood events
     */
    private void loadRecentMoods(boolean personalOnly) {
        String userId = personalOnly ? UserManager.getUserId() : null;
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
     * Updates the bar chart with mood frequency data.
     * Creates and configures a horizontal bar chart showing the count of each mood type.
     *
     * @param moodEvents List of mood events to be visualized
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
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true);
        dataSet.setHighlightEnabled(true);

        // Add value formatter to show count
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f); // Make bars thicker

        // Configure X-axis
        XAxis xAxis = recentMoodsChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setSpaceMin(0.5f);
        xAxis.setAxisLineWidth(1.5f);
        xAxis.setAxisLineColor(Color.BLACK);

        // Configure Y-axis
        recentMoodsChart.getAxisLeft().setAxisMinimum(0f);
        recentMoodsChart.getAxisLeft().setDrawGridLines(true);
        recentMoodsChart.getAxisLeft().setGridColor(Color.LTGRAY);
        recentMoodsChart.getAxisLeft().setGridLineWidth(0.5f);
        recentMoodsChart.getAxisLeft().setTextSize(12f);
        recentMoodsChart.getAxisLeft().setAxisLineWidth(1.5f);
        recentMoodsChart.getAxisLeft().setAxisLineColor(Color.BLACK);

        // Disable right axis
        recentMoodsChart.getAxisRight().setEnabled(false);

        // Other chart settings
        recentMoodsChart.setData(barData);
        recentMoodsChart.setFitBars(true);
        recentMoodsChart.setDrawValueAboveBar(true);
        recentMoodsChart.setMaxVisibleValueCount(10);
        recentMoodsChart.setDrawGridBackground(false);
        recentMoodsChart.setPinchZoom(false);
        recentMoodsChart.setDoubleTapToZoomEnabled(false);

        // Remove description
        recentMoodsChart.getDescription().setEnabled(false);

        // Customize legend
        recentMoodsChart.getLegend().setEnabled(false);

        // Add margin for better readability
        recentMoodsChart.setExtraLeftOffset(15f);
        recentMoodsChart.setExtraBottomOffset(15f);
        recentMoodsChart.setExtraTopOffset(10f);
        recentMoodsChart.setExtraRightOffset(15f);

        // Animate chart
        recentMoodsChart.animateY(1000);
        recentMoodsChart.invalidate();
    }

    /**
     * Retrieves an array of colors corresponding to each mood type.
     * These colors are used for visual consistency in charts.
     *
     * @return Array of integer color values for each mood type
     */
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
     * Displays mood analytics by setting up the pie chart and fading in views.
     *
     * @param moodEvents List of mood events to analyze and visualize
     */
    private void displayAnalytics(List<MoodEvent> moodEvents) {
        setupPieChart(moodEvents);
        fadeInViews();
    }

    /**
     * Creates and animates an emoji rain effect in the background.
     * Randomly generates emoji elements that fall from the top of the screen.
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
     * Sets up the pie chart to display mood type distribution.
     * Configures appearance, animations, and data representation.
     *
     * @param moodEvents List of mood events to analyze for the distribution
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
                    .orElse(null);

            if (moodEvent != null) {
                entries.add(new PieEntry(count, moodType.name()));
                colors.add(moodEvent.getMoodType().getPrimaryColor(requireContext()));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "Mood Distribution");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueLineColor(Color.WHITE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setSliceSpace(3f); // Space between slices

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(moodDistributionChart));

        // Configure the pie chart
        moodDistributionChart.setUsePercentValues(true);
        moodDistributionChart.setEntryLabelTextSize(14f);
        moodDistributionChart.setEntryLabelColor(Color.BLACK);
        moodDistributionChart.setHoleRadius(40f);
        moodDistributionChart.setTransparentCircleRadius(45f);
        moodDistributionChart.setTransparentCircleColor(Color.WHITE);
        moodDistributionChart.setTransparentCircleAlpha(110);
        moodDistributionChart.setCenterText("Mood\nDistribution");
        moodDistributionChart.setCenterTextSize(18f);
        moodDistributionChart.setDrawCenterText(true);

        // Remove legend and description
        moodDistributionChart.getLegend().setEnabled(false);
        moodDistributionChart.getDescription().setEnabled(false);

        moodDistributionChart.setData(pieData);
        moodDistributionChart.setExtraOffsets(20, 10, 20, 10);

        // Add animations
        moodDistributionChart.animateY(1200, Easing.EaseInOutQuad);
        moodDistributionChart.spin(1000, 0, 360f, Easing.EaseInOutQuad);

        moodDistributionChart.invalidate();
    }

    /**
     * Fades in the chart views with animation for a smooth visual experience.
     */
    private void fadeInViews() {
        moodDistributionChart.animate().alpha(1f).setDuration(800).setInterpolator(new DecelerateInterpolator())
                .start();
    }
}