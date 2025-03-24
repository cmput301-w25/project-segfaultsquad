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
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.Manifest;

import com.example.segfaultsquadapplication.Map_api;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.following.Following;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.location.LocationManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import com.google.android.material.chip.ChipGroup;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;
import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import android.graphics.Color;
import android.widget.Toast;
import androidx.navigation.Navigation;

import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

public class MapFragment extends Fragment {
    // Attributes
    private GoogleMap mMap;
    private Location currentLocation;

    // MoodEvent Lists
    private List<MoodEvent> userMoods;
    private List<MoodEvent> filteredUserMoods; // For displaying filtered view
    private Map<String, MoodEvent> followedMoods; // Key: userId, Value: most recent mood
    private List<MoodEvent> filteredFollowedMoods; // For displaying filtered view
    private List<MoodEvent> localMoods;
    private List<MoodEvent> filteredLocalMoods; // For displaying filtered view

    // Tabs
    private static final int TAB_MY_MOODS = 0;
    private static final int TAB_FOLLOWED = 1;
    private static final int TAB_LOCAL = 2;

    // Current selected tab
    private int currentTab = TAB_MY_MOODS;

    // distance in km for local moods
    private static final float LOCAL_RADIUS_KM = 5f;

    private MapView mapView;

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
        LocationManager.prepareLocationProvider(getActivity());
        // init mood lists
        userMoods = new ArrayList<>();
        filteredUserMoods = new ArrayList<>();
        followedMoods = new HashMap<>();
        filteredFollowedMoods = new ArrayList<>();
        localMoods = new ArrayList<>();
        filteredLocalMoods = new ArrayList<>();
        // Load data
        loadMoodData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data every time the map fragment becomes visible
        refreshMoodData();
        updateCurrentLocation();
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize MapView or any other components that require view interaction
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Example of setting map tiles
        mapView.setMultiTouchControls(true);

        // Check and request location permissions if necessary
        enableMyLocation();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Chip group view
        mapChipGroup = view.findViewById(R.id.map_chip_group); // find it
        mapChipGroup.check(R.id.chip_my_moods); // Set default selection
        // Chip click listener
        mapChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_my_moods) {
                // Handle My Mood History selection
                currentTab = TAB_MY_MOODS;
                updateMapMarkers(TAB_MY_MOODS);
            } else if (checkedId == R.id.chip_followed_moods) {
                // Handle Followed Moods selection
                currentTab = TAB_FOLLOWED;
                updateMapMarkers(TAB_FOLLOWED);
            } else if (checkedId == R.id.chip_local_moods) {
                // Handle Local Moods selection
                currentTab = TAB_LOCAL;
                updateMapMarkers(TAB_LOCAL);
            }
        });

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
            showMoodFilterDialog();
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            showReasonFilterDialog();
            toggleFilterMenu();
        });

        // Add clear filters option
        view.findViewById(R.id.clearFilters).setOnClickListener(v -> {
            clearAllFilters();
            toggleFilterMenu();
        });

        // Load user settings
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));

        // Initialize MapView
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Use OpenStreetMap tiles
        mapView.setMultiTouchControls(true);


        // Set default location and zoom level
        mapView.getController().setZoom(15.0);
        enableMyLocation();

        // Add compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        return view;
    }

    /**
     * Refresh all mood data when returning to the map
     */
    private void refreshMoodData() {
        // Clear all filtered lists to ensure we get fresh data
        userMoods.clear();
        filteredUserMoods.clear();
        followedMoods.clear();
        filteredFollowedMoods.clear();
        localMoods.clear();
        filteredLocalMoods.clear();

        // Load all mood data fresh
        loadMoodData();

        // Update the map markers based on the current tab selection
        if (mapChipGroup != null) {
            updateMapMarkers(currentTab);
        }

        // Log that refresh happened
        Log.d("MapFragment", "Refreshed mood data");
    }

    private void loadMoodData() {
        String currentUserId = DbUtils.getUserId();

        // Load user's moods
        ArrayList<MoodEvent> events = new ArrayList<>();
        MoodEventManager.getAllMoodEvents(currentUserId, MoodEventManager.MoodEventFilter.ALL, events,
                isSuccess -> {
                    if (isSuccess) {
                        userMoods.clear();
                        userMoods.addAll(events);
                        // Initialize filtered list with all moods
                        filteredUserMoods.clear();
                        filteredUserMoods.addAll(userMoods);
                        if (mapChipGroup.getCheckedChipId() == R.id.chip_my_moods) {
                            updateMapMarkers(TAB_MY_MOODS);
                        }
                    } else {
                        Toast.makeText(getContext(), "Fail loading my own mood event data", Toast.LENGTH_LONG).show();
                    }
                });

        // Load followed users' moods
        loadFollowedUsersMoods();

        // Load local moods (if location available)
        if (currentLocation != null) {
            loadLocalMoods();
        }
    }

    private void loadFollowedUsersMoods() {
        // First get list of followed users
        String currentUserId = DbUtils.getUserId();
        ArrayList<Following> followedUsers = new ArrayList<>();
        FollowingManager.getAllFollowed(currentUserId, followedUsers, getFldSucc -> {
            if (getFldSucc) {
                // Then get their most recent moods
                followedMoods.clear(); // Clear existing moods
                for (Following flw : followedUsers) {
                    ArrayList<MoodEvent> evts = new ArrayList<>();
                    String uid = flw.getFollowedId();
                    MoodEventManager.getAllMoodEvents(uid,
                            MoodEventManager.MoodEventFilter.MOST_RECENT_1, evts,
                            isSuccess -> {
                                if (evts.size() > 0) {
                                    followedMoods.put(uid, evts.get(0));
                                    // Update filtered list
                                    filteredFollowedMoods.clear();
                                    filteredFollowedMoods.addAll(followedMoods.values());
                                    if (mapChipGroup.getCheckedChipId() == R.id.chip_followed_moods) {
                                        updateMapMarkers(TAB_FOLLOWED);
                                    }
                                }
                            });
                }
            }
            // Could not load list of followed users
            else {
                Toast.makeText(getContext(), "Fail loading the list of followed users", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadLocalMoods() {
        if (currentLocation == null || mapChipGroup == null)
            return;

        // Create a GeoPoint for the current location
        GeoPoint center = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        // Get list of followed users' IDs
        String currentUserId = DbUtils.getUserId();
        ArrayList<Following> followedUsers = new ArrayList<>();
        ArrayList<String> followedUserIds = new ArrayList<>();

        FollowingManager.getAllFollowed(currentUserId, followedUsers, getFldSucc -> {
            if (getFldSucc) {
                // Extract followed user IDs
                for (Following flw : followedUsers) {
                    followedUserIds.add(flw.getFollowedId());
                }

                // Get all moods and filter by distance AND followed users
                ArrayList<MoodEvent> holder = new ArrayList<>();
                MoodEventManager.getAllMoodEvents(null,
                        MoodEventManager.MoodEventFilter.ALL, holder, isSuccess -> {
                            if (isSuccess) {
                                // Clear local moods
                                localMoods.clear();
                                // Add moods nearby AND from followed users to local moods
                                holder.forEach(mood -> {
                                    if (mood.getLocation() != null &&
                                            isWithinRadius(mood.getLocation(), center, LOCAL_RADIUS_KM) &&
                                            followedUserIds.contains(mood.getUserId())) {
                                        localMoods.add(mood);
                                    }
                                });

                                // Initialize filtered list with all local moods
                                filteredLocalMoods.clear();
                                filteredLocalMoods.addAll(localMoods);

                                // Update markers if appropriate
                                if (mapChipGroup.getCheckedChipId() == R.id.chip_local_moods) {
                                    updateMapMarkers(TAB_LOCAL);
                                }
                            } else {
                                Toast.makeText(getContext(), "Failed loading the list of local moods", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    /**
     * method to determine if local moods are within range of display
     *
     * @param point1 user location
     * @param point2 mood location
     * @param radiusKm radius limit for definition of local
     * @return true if within radius, false otherwise
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
     * @param tabPosition the filter being applied index
     */
    private void updateMapMarkers(int tabPosition) {
        if (mapView == null)
            return;

        // Clear existing markers
        mapView.getOverlays().clear();

        // Add compass and location overlays back (they were cleared)
        enableLocationOverlay();
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        List<MoodEvent> moodsToShow = new ArrayList<>();

        switch (tabPosition) {
            case TAB_MY_MOODS:
                moodsToShow.addAll(filteredUserMoods);
                break;
            case TAB_FOLLOWED:
                moodsToShow.addAll(filteredFollowedMoods);
                break;
            case TAB_LOCAL:
                moodsToShow.addAll(filteredLocalMoods);
                break;
        }

        // Add markers for each mood
        for (MoodEvent mood : moodsToShow) {
            if (mood.getLocation() != null) {
                org.osmdroid.util.GeoPoint point = new org.osmdroid.util.GeoPoint(
                        mood.getLocation().getLatitude(),
                        mood.getLocation().getLongitude()
                );

                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Set icon based on mood type
                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.map_icon);
                // You might want to tint the icon with the mood color
                icon.setTint(mood.getMoodType().getPrimaryColor(requireContext()));
                marker.setIcon(icon);

                // Set title as mood type
                marker.setTitle(mood.getMoodType().toString());

                // For followed moods, add the username in the snippet
                if (tabPosition == TAB_FOLLOWED || tabPosition == TAB_LOCAL) {
                    marker.setSubDescription(mood.getUserId()); // Assuming username is accessible
                }

                // Store the mood ID as a related object
                marker.setRelatedObject(mood.getDbFileId());

                // Set up marker click listener
                marker.setOnMarkerClickListener((marker1, mapView) -> {
                    String moodId = (String) marker1.getRelatedObject();
                    if (moodId != null) {
                        navigateToMoodDetails(moodId);
                    }
                    return true; // Return true to consume the event
                });

                mapView.getOverlays().add(marker);
            }
        }

        mapView.invalidate(); // Refresh the map
    }

// 3. Add this new method to the class:
    /**
     * Navigate to the MoodDetails fragment with the selected mood ID
     * @param moodId The ID of the mood to display details for
     */
    private void navigateToMoodDetails(String moodId) {
        Bundle args = new Bundle();
        args.putString("moodId", moodId);

        // Navigate to mood details fragment
        Navigation.findNavController(requireView())
                .navigate(R.id.action_map_to_moodDetails, args);

        // Display a toast message for feedback
        Toast.makeText(getContext(), "Viewing mood details", Toast.LENGTH_SHORT).show();
    }


    private void updateCurrentLocation() {
        AtomicReference<Location> locHolder = new AtomicReference<>();
        LocationManager.getLocation(locHolder, isSuccess -> {
            // A valid location is returned
            if (isSuccess) {
                currentLocation = locHolder.get();
                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();
                Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);
                // Update map with user's location
                updateMapLocation(latitude, longitude);

                // Load local moods now that we have location
                loadLocalMoods();
            }
            // Handle case where a valid location can not be found
            else {
                setDefaultLocation();
            }
        });
    }

    /**
     * Updates the map with the given latitude and longitude.
     */
    private void updateMapLocation(double latitude, double longitude) {
        org.osmdroid.util.GeoPoint osmGeoPoint = new org.osmdroid.util.GeoPoint(latitude, longitude);

        mapView.getController().setCenter(osmGeoPoint);
        enableLocationOverlay();
    }

    /**
     * Sets a default location (e.g., Edmonton) when location retrieval fails.
     */
    private void setDefaultLocation() {
        double defaultLat = 53.52624;
        double defaultLon = -113.52048;

        updateMapLocation(defaultLat, defaultLon);
        Log.w("Location", "Using default location: Edmonton");
    }

    /**
     * Enables the location overlay on the map.
     */
    private void enableLocationOverlay() {
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);
    }


    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateCurrentLocation();
        } else {
            requestLocationPermission();
        }
    }


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

    public void addRedMarker(MapView map, double latitude, double longitude) {
        org.osmdroid.util.GeoPoint osmGeoPoint = new org.osmdroid.util.GeoPoint(latitude, longitude);

        Marker marker = new Marker(map);
        marker.setPosition(osmGeoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Set red icon
        Drawable redIcon = ContextCompat.getDrawable(map.getContext(), R.drawable.map_icon);
        marker.setIcon(redIcon);

        map.getOverlays().add(marker);
        map.invalidate(); // Refresh the map
    }

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

    /**
     * method to toggle filtered view of map
     */
    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * method to apply filter
     *
     * @param filterType filter being applied
     */
    private void applyFilter(String filterType) {
        switch (filterType) {
            case "Last Week":
                filterLastWeek();
                break;
            case "By Mood":
                showMoodFilterDialog();
                break;
            case "By Reason":
                showReasonFilterDialog();
                break;
        }
    }

    /**
     * Filter moods from the last week
     */
    private void filterLastWeek() {
        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -1); // Subtract one week
        Date lastWeekDate = calendar.getTime();

        // Apply filter based on current tab
        switch (currentTab) {
            case TAB_MY_MOODS:
                filteredUserMoods.clear();
                for (MoodEvent mood : userMoods) {
                    if (mood.getTimestamp().toDate().after(lastWeekDate)) {
                        filteredUserMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_MY_MOODS);
                break;

            case TAB_FOLLOWED:
                filteredFollowedMoods.clear();
                for (MoodEvent mood : followedMoods.values()) {
                    if (mood.getTimestamp().toDate().after(lastWeekDate)) {
                        filteredFollowedMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_FOLLOWED);
                break;

            case TAB_LOCAL:
                filteredLocalMoods.clear();
                for (MoodEvent mood : localMoods) {
                    if (mood.getTimestamp().toDate().after(lastWeekDate)) {
                        filteredLocalMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_LOCAL);
                break;
        }
    }

    /**
     * Show dialog for filtering by mood type
     */
    private void showMoodFilterDialog() {
        String[] moods = {"ANGER", "CONFUSION", "DISGUST", "FEAR", "HAPPINESS", "SADNESS", "SHAME", "SURPRISE"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Mood")
                .setItems(moods, (dialog, which) -> {
                    String selectedMood = moods[which];
                    filterByMood(selectedMood);
                });
        builder.show();
    }

    /**
     * Filter moods by mood type
     *
     * @param moodType the mood type to filter by
     */
    private void filterByMood(String moodType) {
        switch (currentTab) {
            case TAB_MY_MOODS:
                filteredUserMoods.clear();
                for (MoodEvent mood : userMoods) {
                    if (mood.getMoodType().name().equals(moodType)) {
                        filteredUserMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_MY_MOODS);
                break;

            case TAB_FOLLOWED:
                filteredFollowedMoods.clear();
                for (MoodEvent mood : followedMoods.values()) {
                    if (mood.getMoodType().name().equals(moodType)) {
                        filteredFollowedMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_FOLLOWED);
                break;

            case TAB_LOCAL:
                filteredLocalMoods.clear();
                for (MoodEvent mood : localMoods) {
                    if (mood.getMoodType().name().equals(moodType)) {
                        filteredLocalMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_LOCAL);
                break;
        }
    }

    /**
     * Show dialog for filtering by reason text
     */
    private void showReasonFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Reason Keyword");

        // a LinearLayout for the EditText
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        // EditText (input field)
        final EditText input = new EditText(getContext());
        input.setHint("Type search word here...");
        // Setting margins programmatically for the input field to match the title's indent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 0, 40, 0);
        input.setLayoutParams(params);
        layout.addView(input); // add the EditText to the layout
        builder.setView(layout); // set the layout as the dialog

        builder.setPositiveButton("OK", (dialog, which) -> {
            String keyword = input.getText().toString();
            filterByReason(keyword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Filter moods by reason text
     *
     * @param keyword the keyword to filter by
     */
    private void filterByReason(String keyword) {
        switch (currentTab) {
            case TAB_MY_MOODS:
                filteredUserMoods.clear();
                for (MoodEvent mood : userMoods) {
                    if (mood.getReasonText() != null &&
                            mood.getReasonText().toLowerCase().contains(keyword.toLowerCase())) {
                        filteredUserMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_MY_MOODS);
                break;

            case TAB_FOLLOWED:
                filteredFollowedMoods.clear();
                for (MoodEvent mood : followedMoods.values()) {
                    if (mood.getReasonText() != null &&
                            mood.getReasonText().toLowerCase().contains(keyword.toLowerCase())) {
                        filteredFollowedMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_FOLLOWED);
                break;

            case TAB_LOCAL:
                filteredLocalMoods.clear();
                for (MoodEvent mood : localMoods) {
                    if (mood.getReasonText() != null &&
                            mood.getReasonText().toLowerCase().contains(keyword.toLowerCase())) {
                        filteredLocalMoods.add(mood);
                    }
                }
                updateMapMarkers(TAB_LOCAL);
                break;
        }
    }

    /**
     * Clear all filters and reset to showing all moods
     */
    private void clearAllFilters() {
        switch (currentTab) {
            case TAB_MY_MOODS:
                filteredUserMoods.clear();
                filteredUserMoods.addAll(userMoods);
                updateMapMarkers(TAB_MY_MOODS);
                break;

            case TAB_FOLLOWED:
                filteredFollowedMoods.clear();
                filteredFollowedMoods.addAll(followedMoods.values());
                updateMapMarkers(TAB_FOLLOWED);
                break;

            case TAB_LOCAL:
                filteredLocalMoods.clear();
                filteredLocalMoods.addAll(localMoods);
                updateMapMarkers(TAB_LOCAL);
                break;
        }
    }
}
