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
import com.example.segfaultsquadapplication.impl.following.Following;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.location.LocationManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent.MoodType;
import com.example.segfaultsquadapplication.impl.user.User;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import com.google.android.material.chip.ChipGroup;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Calendar;
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
import org.osmdroid.util.BoundingBox;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;

//import org.osmdroid.util.GeoPoint;

public class MapFragment extends Fragment {
    // Attributes
    private GoogleMap mMap;
    private Location currentLocation;

    // MoodEvent Lists
    private List<MoodEvent> userMoods;
    private Map<String, MoodEvent> followedMoods; // Key: userId, Value: most recent mood
    private List<MoodEvent> localMoods;
    private FirebaseAuth auth;
    private FirebaseFirestore db;


    // Tabs
    private static final int TAB_MY_MOODS = 0;
    private static final int TAB_FOLLOWED = 1;
    private static final int TAB_LOCAL = 2;

    // distance in km for local moods
    private static final float LOCAL_RADIUS_KM = 5f;

    private MapView mapView;

    private ChipGroup mapChipGroup;

    // Add these as class members for filtering on map screen
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private static List<MoodEvent> allMoods = new ArrayList<>();
    private List<MoodEvent> filteredFollowedMoods = new ArrayList<>();
    private Map<String, Integer> locationOffsets = new HashMap<>();
    // Current selected tab
    private int currentTab = TAB_MY_MOODS;

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
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
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
        // Add clear filters option
        view.findViewById(R.id.clearFilters).setOnClickListener(v -> {
            //clearAllFilters();
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
        mapView.setMinZoomLevel(3.0);
        mapView.setMaxZoomLevel(20.0);
        BoundingBox boundingBox = new BoundingBox(85.0, 180.0, -85.0, -180.0); // North, East, South, West
        mapView.setScrollableAreaLimitDouble(boundingBox);
        enableMyLocation();

        // Add compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
        addRedMarker(mapView, 53.52672, -113.52877);
        Map_api.getCoordinates("10922 88 Ave NW, Edmonton, AB T6G 0Z1", new Map_api.GeocodingListener() {
            @Override
            public void onLocationFound(double latitude, double longitude) {
                // Print latitude and longitude to Logcat
                Log.d("Coordinates", latitude + ", " + longitude);

                // Now you can use these coordinates anywhere
                addRedMarker(mapView, latitude, longitude);
            }

            @Override
            public void onError(String error) {
                Log.e("Geocoding", "Error: " + error);
            }
        });


        //addRedMarker(mapView, 53.5461, -113.4937);

        return view;
    }

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
        // debugging
        Log.d("MoodHistory", "Loading moods for user: " + DbUtils.getUserId());
        // Clear previous moods and markers
        clearMoodMarkers(); // Ensures old moods and markers are removed
        // Clear allMoods to avoid adding old data
        allMoods.clear();  // Clear the list of all moods
        // get the moods
        ArrayList<MoodEvent> temp = new ArrayList<>();
        MoodEventManager.getAllMoodEvents(DbUtils.getUserId(), MoodEventManager.MoodEventFilter.ALL, temp, isSuccess -> {
            if (isSuccess) {
                allMoods.clear(); // Clear previous moods
                // debugging
                Log.d("MoodHistory", "Number of moods retrieved: " + temp.size());

                for (MoodEvent mood : temp) {
                    allMoods.add(mood); // add to arraylist
                    Log.d("MoodHistory",
                            "Loaded mood: " + mood.getMoodType() + " with ID: " + mood.getDbFileId());
                    // Add each mood as a marker on the map
                    if (mood.getLocation() != null) {
                        addMoodMarkerToMap(mood);
                    }
                }
            } else {
                Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFollowingData() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Debugging log
        Log.d("FollowingList", "Loading followed users for: " + currentUserId);

        // Clear previous moods and markers
        clearMoodMarkers(); // Ensures old moods and markers are removed

        // Clear allMoods to avoid adding old data
        allMoods.clear();  // Clear the list of all moods

        // Query the "following" collection to get the list of users the current user follows
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followedUserIds = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        followedUserIds.add(document.getString("followedId"));
                    }

                    Log.d("FollowingList", "Number of followed users: " + followedUserIds.size());

                    // Fetch mood data for each followed user
                    for (String userId : followedUserIds) {
                        // Skip adding the current user's own moods
                        if (userId.equals(currentUserId)) {
                            continue; // Skip the current user's moods
                        }

                        MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, new ArrayList<>(), isSuccess -> {
                            if (isSuccess) {
                                Log.d("FollowingList", "Loaded moods for user: " + userId);

                                // Collect moods into allMoods list
                                ArrayList<MoodEvent> tempMoods = new ArrayList<>();
                                tempMoods.addAll(allMoods);  // Copy current moods into tempMoods

                                // Now add moods for this user into the allMoods
                                MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, tempMoods, isLoaded -> {
                                    if (isLoaded) {
                                        Log.d("FollowingList", "Loaded " + tempMoods.size() + " moods for user: " + userId);

                                        // Add each mood as a marker on the map
                                        for (MoodEvent mood : tempMoods) {
                                            if (mood.getLocation() != null) {
                                                addMoodMarkerToMap(mood); // Add marker to map
                                            }
                                        }
                                    } else {
                                        Log.e("FollowingList", "Error loading moods for user: " + userId);
                                    }
                                });
                            } else {
                                Log.e("FollowingList", "Error loading moods for user: " + userId);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingList", "Error fetching following data", e);
                });
    }

    private void MoodsWithinRadius(float radius) {
        // Ensure that currentLocation is available
        if (currentLocation == null) {
            Log.e("Location", "Current location is not available");
            return; // Exit if location is not available
        }

        String currentUserId = auth.getCurrentUser().getUid();

        // Debugging log
        Log.d("FollowingList", "Loading followed users for: " + currentUserId);

        // Clear previous moods and markers
        clearMoodMarkers(); // Ensures old moods and markers are removed

        // Clear allMoods to avoid adding old data
        allMoods.clear();  // Clear the list of all moods

        // Query the "following" collection to get the list of users the current user follows
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followedUserIds = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        followedUserIds.add(document.getString("followedId"));
                    }

                    Log.d("FollowingList", "Number of followed users: " + followedUserIds.size());

                    // Fetch mood data for each followed user
                    for (String userId : followedUserIds) {
                        // Skip adding the current user's own moods
                        if (userId.equals(currentUserId)) {
                            continue; // Skip the current user's moods
                        }

                        MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, new ArrayList<>(), isSuccess -> {
                            if (isSuccess) {
                                Log.d("FollowingList", "Loaded moods for user: " + userId);

                                ArrayList<MoodEvent> tempMoods = new ArrayList<>();
                                tempMoods.addAll(allMoods); // Copy current moods into tempMoods

                                // Fetch moods for this user and add to the list
                                MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, tempMoods, isLoaded -> {
                                    if (isLoaded) {
                                        Log.d("FollowingList", "Loaded " + tempMoods.size() + " moods for user: " + userId);

                                        // Filter moods to be within the specified radius
                                        ArrayList<MoodEvent> nearbyMoods = new ArrayList<>();
                                        for (MoodEvent mood : tempMoods) {
                                            if (mood.getLocation() != null) {
                                                Location moodLocation = new Location("");
                                                moodLocation.setLatitude(mood.getLocation().getLatitude());
                                                moodLocation.setLongitude(mood.getLocation().getLongitude());

                                                // Calculate distance from current user's location
                                                float distance = currentLocation.distanceTo(moodLocation); // Distance in meters

                                                // Only consider moods within the specified radius
                                                if (distance <= radius) {
                                                    nearbyMoods.add(mood);
                                                }
                                            }
                                        }

                                        // Sort moods by most recent timestamp
                                        if (!nearbyMoods.isEmpty()) {
                                            Collections.sort(nearbyMoods, (mood1, mood2) -> mood2.getTimestamp().compareTo(mood1.getTimestamp()));

                                            // Add each filtered mood marker to the map
                                            for (MoodEvent mood : nearbyMoods) {
                                                addMoodMarkerToMap(mood); // Add marker to map
                                            }
                                        }

                                    } else {
                                        Log.e("FollowingList", "Error loading moods for user: " + userId);
                                    }
                                });
                            } else {
                                Log.e("FollowingList", "Error loading moods for user: " + userId);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingList", "Error fetching following data", e);
                });
    }



    private void addMoodMarkerToMap(MoodEvent mood) {
        if (mood.getLocation() == null) {
            Log.e("MoodLocation", "Mood has no location: " + mood.getMoodType());
            return;
        }

        Log.d("MoodLocation", "Mood: " + mood.getMoodType() +
                ", Lat: " + mood.getLocation().getLatitude() +
                ", Lng: " + mood.getLocation().getLongitude());
        //org.osmdroid.util.GeoPoint osmGeoPoint = new org.osmdroid.util.GeoPoint(mood.getLocation().getLatitude(), mood.getLocation().getLongitude());
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
        marker.setTitle("user1:" + mood.getMoodType());

        // Add marker to the map
        mapView.getOverlays().add(marker);
        mapView.invalidate(); // Refresh the map

        final boolean[] isInfoWindowShown = {false};

        marker.setOnMarkerClickListener((clickedMarker, mapView1) -> {
            if (!isInfoWindowShown[0]) {
                clickedMarker.showInfoWindow(); // Show the info window
                isInfoWindowShown[0] = true;   // Mark it as shown
            }

            // Use a Handler to clear the title after 3 seconds
            Handler handler = new Handler();
            handler.removeCallbacksAndMessages(null); // Clear any previous delayed messages

            // Clear the title after 3 seconds and close the info window
            handler.postDelayed(() -> {
                clickedMarker.closeInfoWindow(); // Close the info window after clearing the title
                isInfoWindowShown[0] = false;  // Mark it as not shown
            }, 3000);  // 3 seconds delay

            // Return true to indicate that the click event has been handled
            return true;
        });

        // Center the map on the mood location
        //updateMapLocation(mood.getLocation().getLatitude(), mood.getLocation().getLongitude());
    }


    // Create emoji-based marker
    private Drawable createEmojiDrawable(String emoji) {
        // Create a TextView to display the emoji
        TextView textView = new TextView(getContext());
        textView.setText(emoji);
        textView.setTextSize(24);  // Adjust the emoji size (you can make it smaller or larger)
        textView.setTextColor(Color.BLACK);  // You can customize this color

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
     * method to determine if local moods are within range of display
     *
     * @param point1   user location
     * @param point2   mood location
     * @param radiusKm radius limit for definition of local
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
     * @param tabPosition the filter being applied index
     */

    private void updateMapMarkers(int tabPosition) {
        if (mapView == null)
            return;

        //mapView.clearMarkers();
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
//                mapView.addMarker(x, y, color, mood.getMoodType().toString());
            }
        }
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
     * @param filterType filter being applied
     */
    private void applyFilter(String filterType) {
        switch (filterType) {
            case "All Followers":
                loadFollowingData();
                break;
            case "My Moods(default)":
                loadMoodData();
                break;
            case "Followers in 5km":
                updateCurrentLocation();
                MoodsWithinRadius(5000);
                break;
        }
    }
    private void clearMoodMarkers() {
        mapView.getOverlays().clear(); // Remove all markers from the map
        mapView.invalidate(); // Refresh the map view
        allMoods.clear(); // Clear the list of mood events
    }
}

    /**
     * Filter moods from the last week
     */
