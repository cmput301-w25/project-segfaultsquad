package com.example.segfaultsquadapplication.display.map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// imports
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.os.Handler;

import com.example.segfaultsquadapplication.Map_api;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.location.LocationManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.util.Log;
import com.google.android.material.chip.ChipGroup;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.graphics.Color;
import android.widget.Toast;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.util.BoundingBox;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

/**
 * This fragment displays a map where users can view mood events associated with themselves and their followers. It handles location permissions, mood filtering, and mood event loading from Firestore.
 * Outstanding Issues: None
 */
public class MapFragment extends Fragment {
    // Attributes
    private Location currentLocation;

    // MoodEvent Lists
    private List<MoodEvent> userMoods;
    private Map<String, MoodEvent> followedMoods; // Key: userId, Value: most recent mood
    private List<MoodEvent> localMoods;

    private MapView mapView;

    // Add these as class members for filtering on map screen
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private static List<MoodEvent> allMoods = new ArrayList<>();
    private Map<String, Integer> locationOffsets = new HashMap<>();

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
        followedMoods = new HashMap<>();
        localMoods = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize filter views
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);

        // Setup filter button click listener
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        // Setup filter option click listeners
        view.findViewById(R.id.filter1).setOnClickListener(v -> {
            applyFilter("All Followers");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter2).setOnClickListener(v -> {
            applyFilter("My Moods(default)");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            applyFilter("Followers in 5km");
            toggleFilterMenu();
        });

        // Load user settings
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        // Initialize MapView
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Use OpenStreetMap tiles
        mapView.setMultiTouchControls(true);

        // Set default location and zoom level
        mapView.getController().setZoom(18.0);
        mapView.setMinZoomLevel(3.0);
        mapView.setMaxZoomLevel(20.0);
        BoundingBox boundingBox = new BoundingBox(85.0, 180.0, -85.0, -180.0); // map restrictions
        mapView.setScrollableAreaLimitDouble(boundingBox);
        enableMyLocation();

        // Add compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(),
                new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
        return view;
    }

    /**
     * listener of sorts. Executes code dependeant on fragment load-in
     * 
     * @param view
     *                           the view being created
     * @param savedInstanceState
     *                           saved instance for caching
     */
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize MapView or any other components that require view interaction
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Example of setting map tiles
        mapView.setMultiTouchControls(true);

        // Check and request location permissions if necessary
        enableMyLocation();

        loadMoodData();
    }

    /**
     * helper method to get this user's moods, sorted in reverse chronological order
     */
    private void loadMoodData() {
        String currentUserId = DbUtils.getUserId();

        // Debugging log
        Log.d("MoodHistory", "Loading moods for user: " + currentUserId);

        // Load the user details first
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder, isSuccess -> {
            if (!isSuccess || userHolder.get() == null) {
                Log.e("MoodHistory", "Failed to load user data.");
                Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                return;
            }

            User currentUser = userHolder.get();
            Log.d("MoodHistory", "Loaded user: " + currentUser.getUsername());

            // Now fetch the mood events for this user
            ArrayList<MoodEvent> temp = new ArrayList<>();
            MoodEventManager.getAllMoodEvents(currentUserId, MoodEventManager.MoodEventFilter.ALL, temp,
                    moodLoadSuccess -> {
                        if (!moodLoadSuccess) {
                            Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Clear previous moods and markers on success
                        clearMoodEvents();
                        Log.d("MoodHistory", "Number of moods retrieved: " + temp.size());

                        for (MoodEvent mood : temp) {
                            allMoods.add(mood); // Add to arraylist
                            Log.d("MoodHistory",
                                    "Loaded mood: " + mood.getMoodType() + " with ID: " + mood.getDbFileId());

                            // Add each mood as a marker on the map
                            if (mood.getLocation() != null) {
                                addMoodMarkerToMap(mood, currentUser);
                            }
                        }
                    });
        });
    }

    /**
     * helper method to load in following user's data
     */
    private void loadFollowingData() {
        String currentUserId = UserManager.getUserId();

        // Debugging log
        Log.d("FollowingList", "Loading followed users for: " + currentUserId);

        // Get the user's following list
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder, isSuccess -> {
            if (!isSuccess || userHolder.get() == null) {
                Log.e("FollowingList", "Failed to load current user data.");
                return;
            }

            List<String> followedUserIds = new ArrayList<>(userHolder.get().getFollowing());

            if (followedUserIds.isEmpty()) {
                // If there are no followed users, show a reminder
                Log.d("FollowingList", "No followed users currently.");
                Toast.makeText(requireContext(), "You have no followed user currently.", Toast.LENGTH_LONG).show();
                return; // Exit as there are no followed users to load moods for
            }

            Log.d("FollowingList", "Number of followed users: " + followedUserIds.size());

            // Fetch mood data for each followed user
            for (String userId : followedUserIds) {
                if (userId.equals(currentUserId)) {
                    continue; // Skip current user's own moods
                }

                // Load followed user details first
                AtomicReference<User> followedUserRef = new AtomicReference<>();
                UserManager.loadUserData(userId, followedUserRef, userLoadSuccess -> {
                    if (!userLoadSuccess || followedUserRef.get() == null) {
                        Log.e("FollowingList", "Error loading user data for: " + userId);
                        return;
                    }

                    User followedUser = followedUserRef.get();
                    Log.d("FollowingList", "Loaded user: " + userId + " -> " + followedUser.getUsername());

                    // Now fetch mood events for this user
                    ArrayList<MoodEvent> evtHolder = new ArrayList<>();
                    MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL,
                            evtHolder, isMoodEvtSuccess -> {
                                if (!isMoodEvtSuccess) {
                                    Log.e("FollowingList", "Error loading moods for user: " + userId);
                                    return;
                                }

                                Log.d("FollowingList", "Loaded moods for user: " + userId);

                                // Collect moods into allMoods list
                                allMoods.addAll(evtHolder);

                                // Add each mood as a marker on the map with user details
                                for (MoodEvent mood : evtHolder) {
                                    if (mood.getLocation() != null) {
                                        addMoodMarkerToMap(mood, followedUser);
                                    }
                                }
                            });
                });
            }
        });
    }

    /**
     * method to restrict mood event radius on map
     * 
     * @param radius
     *               the premitted radius in km
     */
    private void MoodsWithinRadius(float radius) {
        // Ensure that currentLocation is available
        if (currentLocation == null) {
            Log.e("Location", "Current location is not available");
            return; // Exit if location is not available
        }

        String currentUserId = UserManager.getUserId();

        // Debugging log
        Log.d("FollowingList", "Loading followed users for: " + currentUserId);

        // Get the list of users the current user follows
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder, isSuccess -> {
            if (!isSuccess || userHolder.get() == null) {
                Log.e("FollowingList", "Failed to load current user data.");
                return;
            }

            List<String> followedUserIds = new ArrayList<>(userHolder.get().getFollowing());

            if (followedUserIds.isEmpty()) {
                // If there are no followers, show a reminder
                Log.d("FollowingList", "No followers currently.");
                Toast.makeText(requireContext(), "You have no follower currently.", Toast.LENGTH_LONG).show();
                return; // Exit as there are no followers to load moods for
            }

            Log.d("FollowingList", "Number of followed users: " + followedUserIds.size());

            // Fetch mood data for each followed user
            for (String userId : followedUserIds) {
                if (userId.equals(currentUserId)) {
                    continue; // Skip current user's own moods
                }

                // Load user details first
                AtomicReference<User> followedUserRef = new AtomicReference<>();
                UserManager.loadUserData(userId, followedUserRef, userLoadSuccess -> {
                    if (!userLoadSuccess || followedUserRef.get() == null) {
                        Log.e("FollowingList", "Error loading user data for: " + userId);
                        return;
                    }

                    User followedUser = followedUserRef.get();
                    Log.d("FollowingList", "Loaded user: " + userId + " -> " + followedUser.getUsername());

                    // Now fetch mood events for this user
                    ArrayList<MoodEvent> evtHolder = new ArrayList<>();
                    MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL,
                            evtHolder, isMoodEvtSuccess -> {
                                if (!isMoodEvtSuccess) {
                                    Log.e("FollowingList", "Error loading moods for user: " + userId);
                                    return;
                                }

                                Log.d("FollowingList", "Loaded moods for user: " + userId);

                                // Filter moods within the specified radius
                                for (MoodEvent mood : evtHolder) {
                                    if (mood.getLocation() != null) {
                                        Location moodLocation = new Location("");
                                        moodLocation.setLatitude(mood.getLocation().getLatitude());
                                        moodLocation.setLongitude(mood.getLocation().getLongitude());

                                        // Ignore moods outside of the specified radius, in meters
                                        float distance = currentLocation.distanceTo(moodLocation);
                                        if (distance > radius)
                                            continue;

                                        // Record marker & add to map
                                        allMoods.add(mood);
                                        addMoodMarkerToMap(mood, followedUser);
                                    }
                                }
                            });
                });
            }
        });
    }

    /**
     * adds the mood event markers to the map
     * 
     * @param mood
     *             the mood events
     * @param user
     *             the user
     */
    private void addMoodMarkerToMap(MoodEvent mood, User user) {
        if (mood.getLocation() == null) {
            Log.e("MoodLocation", "Mood has no location: " + mood.getMoodType());
            return;
        }

        Log.d("MoodLocation", "Mood: " + mood.getMoodType() +
                ", Lat: " + mood.getLocation().getLatitude() +
                ", Lng: " + mood.getLocation().getLongitude());
        double lat = mood.getLocation().getLatitude();
        double lon = mood.getLocation().getLongitude();

        // Create a unique key for the location (latitude and longitude)
        String locationKey = lat + "," + lon;

        // Get the number of markers already added at this location
        int markerCount = locationOffsets.getOrDefault(locationKey, 0);

        // Set a fixed offset value for each marker
        double offsetIncrement = 0.00035; // Adjust for how much to offset each subsequent marker
        double newLon = lon + markerCount * offsetIncrement;

        // Increment the count for the next marker at this location
        locationOffsets.put(locationKey, markerCount + 1);

        // Create the GeoPoint with the fixed offset
        org.osmdroid.util.GeoPoint osmGeoPoint = new org.osmdroid.util.GeoPoint(lat, newLon);

        Marker marker = new Marker(mapView);
        marker.setPosition(osmGeoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Use emoji as marker icon
        marker.setIcon(createEmojiDrawable(mood.getMoodType().getEmoticon()));

        // Set a title (mood type) when clicking the marker
        marker.setTitle(user.getUsername() + ": " + mood.getMoodType());

        // Add marker to the map
        mapView.getOverlays().add(marker);
        mapView.invalidate(); // Refresh the map

        final boolean[] isInfoWindowShown = { false };

        marker.setOnMarkerClickListener((clickedMarker, mapView1) -> {
            if (!isInfoWindowShown[0]) {
                clickedMarker.showInfoWindow(); // Show the info window
                isInfoWindowShown[0] = true; // Mark it as shown
            }

            // Use a Handler to clear the title after 3 seconds
            Handler handler = new Handler();
            handler.removeCallbacksAndMessages(null); // Clear any previous delayed messages

            // Clear the title after 3 seconds and close the info window
            handler.postDelayed(() -> {
                clickedMarker.closeInfoWindow(); // Close the info window after clearing the title
                isInfoWindowShown[0] = false; // Mark it as not shown
            }, 3000); // 3 seconds delay

            // Return true to indicate that the click event has been handled
            return true;
        });
    }

    /**
     * Clears the previously displayed mood events and their consequences,
     * e.g. the "mood count" around the same position.
     */
    private void clearMoodEvents() {
        // Clear markers and registered moods
        clearMoodMarkers();
        allMoods.clear();
        // Clear local mood counts
        locationOffsets.clear();
    }

    /**
     * Create emoji-based marker
     * 
     * @param emoji
     *              the emoji to render
     * @return
     *         returns drawable to present on screen
     */
    private Drawable createEmojiDrawable(String emoji) {
        // Create a TextView to display the emoji
        TextView textView = new TextView(getContext());
        textView.setText(emoji);
        textView.setTextSize(30); // Adjust the emoji size
        textView.setTextColor(Color.BLACK);

        // Create a larger Bitmap to accommodate the emoji properly
        int markerWidth = 100; // Increase the width (adjust as necessary)
        int markerHeight = 100; // Increase the height (adjust as necessary)
        Bitmap bitmap = Bitmap.createBitmap(markerWidth, markerHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Set the TextView's layout and draw the emoji on the canvas
        textView.layout(0, 0, markerWidth, markerHeight);
        textView.draw(canvas);

        // Return the Drawable
        return new BitmapDrawable(getResources(), bitmap);
    }

    /**
     * method to update current location
     */
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
            }
            // Handle case where a valid location can not be found
            else {
                setDefaultLocation();
            }
        });
    }

    /**
     * Updates the map with the given latitude and longitude.
     * 
     * @param latitude
     *                  the latitude (double)
     * @param longitude
     *                  the longtitude (double)
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
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()),
                mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);
    }

    /**
     * helper method to enable user location
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateCurrentLocation();
        } else {
            requestLocationPermission();
        }
    }

    /**
     * helper method to request user location premissions
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show an explanation to the user
            showLocationPermissionRationale();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * ui popup for permission methods
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
                    // Keep asking the user for permission again
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
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
     * method to apply filter
     *
     * @param filterType
     *                   filter being applied
     */
    private void applyFilter(String filterType) {
        switch (filterType) {
            case "All Followers":
                loadFollowingData();
                break;
            case "Followers in 5km":
                updateCurrentLocation();
                MoodsWithinRadius(5000);
                break;
            case "My Moods(default)":
            default:
                loadMoodData();
                break;
        }
    }

    /**
     * helper method to clear present marker
     */
    private void clearMoodMarkers() {
        // Get the current overlays from the map
        List<Overlay> overlays = mapView.getOverlays();

        // Find the MyLocationNewOverlay (location overlay) if it exists
        MyLocationNewOverlay locationOverlay = null;
        for (Overlay overlay : overlays) {
            if (overlay instanceof MyLocationNewOverlay) {
                locationOverlay = (MyLocationNewOverlay) overlay;
                break;
            }
        }

        // Clear all other overlays from the map
        mapView.getOverlays().clear();

        // If a location overlay exists, add it back to the map
        if (locationOverlay != null) {
            mapView.getOverlays().add(locationOverlay);
        }

        // Refresh the map view
        mapView.invalidate();

        // Clear the list of mood events
        allMoods.clear();
    }
}