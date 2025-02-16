package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class MyMoodHistoryFragment extends Fragment {
    private ImageButton filterButton;
    private CardView filterMenu;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_mood_history, container, false);

        // Filter button and menu setup
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);

        filterButton.setOnClickListener(v -> {
            filterMenu.setVisibility(filterMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // Filter options handling
        TextView filter1 = view.findViewById(R.id.filter1);
        filter1.setOnClickListener(v -> {
            // Handle filter 1: Last Week
            filterMenu.setVisibility(View.GONE);  // Hide menu after selection
        });

        TextView filter2 = view.findViewById(R.id.filter2);
        filter2.setOnClickListener(v -> {
            // Handle filter 2: By Mood
            filterMenu.setVisibility(View.GONE);  // Hide menu after selection
        });

        TextView filter3 = view.findViewById(R.id.filter3);
        filter3.setOnClickListener(v -> {
            // Handle filter 3: By Reason
            filterMenu.setVisibility(View.GONE);  // Hide menu after selection
        });

        return view;
    }
}