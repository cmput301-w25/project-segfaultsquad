/**
 * Classname: AddMoodFragment
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// imports
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.following.Following;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.GeoPoint;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.chip.ChipGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment {
    // Attributes
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // MoodEvent Lists
    private List<MoodEvent> userMoods;
    private Map<String, MoodEvent> followedMoods; // Key: userId, Value: most recent mood
    private List<MoodEvent> localMoods;

    // Tabs
    private static final int TAB_MY_MOODS = 0;
    private static final int TAB_FOLLOWED = 1;
    private static final int TAB_LOCAL = 2;

    // distance in km for local moods
    private static final float LOCAL_RADIUS_KM = 5f;

    private MapPlaceholderView mapView;

    private ChipGroup mapChipGroup;

    // Add these as class members for filtering on map screen
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;

    // permissions handling
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        // init mood lists
        userMoods = new ArrayList<>();
        followedMoods = new HashMap<>();
        localMoods = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // chip group view
        mapChipGroup = view.findViewById(R.id.map_chip_group); // find it
        mapChipGroup.check(R.id.chip_my_moods); // Set default selection
        // chip click listener
        mapChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_my_moods) {
                // Handle My Mood History selection
                updateMapMarkers(TAB_MY_MOODS);
            } else if (checkedId == R.id.chip_followed_moods) {
                // Handle Followed Moods selection
                updateMapMarkers(TAB_FOLLOWED);
            } else if (checkedId == R.id.chip_local_moods) {
                // Handle Local Moods selection
                updateMapMarkers(TAB_LOCAL);
            }
        });

        // setup map
        // TODO: get the actual map working and replace this
        setupPlaceholderMap(view);

        // Initialize filter views
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);

        // Setup filter button click listener
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        // Setup filter option click listeners
        view.findViewById(R.id.filter1).setOnClickListener(v -> {
            applyFilter("Last Week");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter2).setOnClickListener(v -> {
            applyFilter("By Mood");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            applyFilter("By Reason");
            toggleFilterMenu();
        });

        return view;
    }

    /**
     * method to setup placeholder map view
     * 
     * @param view
     */
    // TODO: get the actual map working and refactor this bit
    private void setupPlaceholderMap(View view) {
        mapView = view.findViewById(R.id.map_placeholder);
    }

    /**
     * method to load in mood data
     */
    private void loadMoodData() {
        String currentUserId = DbUtils.getUserId();

        // Load user's moods
        userMoods.clear();
        MoodEventManager.getAllMoodEvents(currentUserId, MoodEventManager.MoodEventFilter.ALL, userMoods, success -> {
            if (success) {
                if (mapChipGroup.getCheckedChipId() == R.id.chip_my_moods) {
                    updateMapMarkers(TAB_MY_MOODS);
                }
            } else {
                Toast.makeText(getContext(), "Error retrieving my mood events", Toast.LENGTH_SHORT).show();
            }
        });

        // Load followed users' moods
        loadFollowedUsersMoods();

        // Load local moods (if location available)
        if (currentLocation != null) {
            loadLocalMoods();
        }
    }

    /**
     * method to handle user moods on map
     * 
     * @param snapshot
     *                 snapshot of the query to get docs from db
     */
    private void handleUserMoods(QuerySnapshot snapshot) {
        userMoods.clear();
        for (var doc : snapshot.getDocuments()) {
            MoodEvent mood = doc.toObject(MoodEvent.class);
            if (mood != null) {
                userMoods.add(mood);
            }
        }
        if (mapChipGroup.getCheckedChipId() == R.id.chip_my_moods) {
            updateMapMarkers(TAB_MY_MOODS);
        }
    }

    /**
     * method to load in user's followed user's recent moods
     */
    private void loadFollowedUsersMoods() {
        // First get list of followed users
        String currentUserId = DbUtils.getUserId();

        List<Following> followedUsers = new ArrayList<>();
        FollowingManager.getAllFollowed(currentUserId, followedUsers, (isSuccess) -> {
            if (isSuccess) {
                // Get their most recent mood events
                for (Following following : followedUsers) {
                    ArrayList<MoodEvent> mostRecentEvent = new ArrayList<>(1);
                    String followedUserId = following.getFollowedId();
                    MoodEventManager.getAllMoodEvents(followedUserId, MoodEventManager.MoodEventFilter.MOST_RECENT_1,
                            mostRecentEvent, (succeed) -> {
                                if (succeed && mostRecentEvent.size() != 0) {
                                    followedMoods.put(followedUserId, mostRecentEvent.get(0));
                                    if (mapChipGroup.getCheckedChipId() == R.id.chip_followed_moods) {
                                        updateMapMarkers(TAB_FOLLOWED);
                                    }
                                }
                                else {
                                    Toast.makeText(getContext(), "Error getting mood event", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
            else {
                Toast.makeText(getContext(), "Error getting followed users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * method to load in local moods for map
     */
    private void loadLocalMoods() {
        if (currentLocation == null || mapChipGroup == null)
            return;

        // Create a GeoPoint for the current location
        GeoPoint center = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        // Get all moods and filter by distance
        ArrayList<MoodEvent> holder = new ArrayList<>();
        MoodEventManager.getAllMoodEvents(null, MoodEventManager.MoodEventFilter.ALL, holder, (isSuccess) -> {
            if (isSuccess) {
                localMoods.clear();
                for (MoodEvent mood : holder) {
                    if (mood.getLocation() != null &&
                            isWithinRadius(mood.getLocation(), center, LOCAL_RADIUS_KM)) {
                        localMoods.add(mood);
                    }
                }
                if (mapChipGroup.getCheckedChipId() == R.id.chip_local_moods) {
                    updateMapMarkers(TAB_LOCAL);
                }
            }
            else {
                Toast.makeText(getContext(), "Failed to get local moods", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * method to determine if local moods are within range of display
     * 
     * @param point1
     *                 user location
     * @param point2
     *                 mood location
     * @param radiusKm
     *                 radius limit for definition of local
     * @return
     */
    private boolean isWithinRadius(GeoPoint point1, GeoPoint point2, float radiusKm) {
        float[] results = new float[1];
        Location.distanceBetween(
                point1.getLatitude(), point1.getLongitude(),
                point2.getLatitude(), point2.getLongitude(),
                results);
        return results[0] <= radiusKm * 1000; // Convert km to meters
    }

    /**
     * method to update the mood markers on the map based on filter applied
     * 
     * @param tabPosition
     *                    the filter being applied index
     */
    private void updateMapMarkers(int tabPosition) {
        if (mapView == null)
            return;

        mapView.clearMarkers();
        List<MoodEvent> moodsToShow = new ArrayList<>();

        switch (tabPosition) {
            case TAB_MY_MOODS:
                moodsToShow.addAll(userMoods);
                break;
            case TAB_FOLLOWED:
                moodsToShow.addAll(followedMoods.values());
                break;
            case TAB_LOCAL:
                moodsToShow.addAll(localMoods);
                break;
        }

        // Add markers for each mood
        for (MoodEvent mood : moodsToShow) {
            if (mood.getLocation() != null) {
                // Convert GeoPoint to relative position (0-1 range)
                float x = (float) ((mood.getLocation().getLongitude() + 180) / 360);
                float y = (float) ((mood.getLocation().getLatitude() + 90) / 180);

                int color = mood.getMoodType().getPrimaryColor(requireContext());
                mapView.addMarker(x, y, color, mood.getMoodType().toString());
            }
        }
    }

    /**
     * Method to update the users current location
     */
    private void updateCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        updateMapMarkers(mapChipGroup.getCheckedChipId() == R.id.chip_local_moods ? TAB_LOCAL : -1);
                    }
                });
    }

    /**
     * helper method to enable locaiton
     */
    private void enableMyLocation() {
        if (mMap == null)
            return;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            updateCurrentLocation();
        } else {
            requestLocationPermission();
        }
    }

    /**
     * helper method to request user permissions for location
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show an explanation to the user
            // You could show a DialogFragment here
            showLocationPermissionRationale();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * another helper method to request user location permissions
     */
    private void showLocationPermissionRationale() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Location Permission Required")
                .setMessage(
                        "The app needs location permission to show nearby moods and attach location to your mood events.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // TODO: handle this here
                    // Handle the case where user doesn't grant permission
                    // Maybe show a message or disable location-dependent features
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrentLocation();
    }

    /**
     * method to toggle filtered view of map
     */
    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * method to apply filter (INCORRECT)
     * 
     * @param filterType
     *                   filter being applied
     */
    private void applyFilter(String filterType) {
        // TODO: Implement filter logic
        switch (filterType) {
            case "Last Week":
                // Filter moods from last week
                break;
            case "By Mood":
                // Show mood type selector
                break;
            case "By Reason":
                // Show reason selector
                break;
        }
    }
}
