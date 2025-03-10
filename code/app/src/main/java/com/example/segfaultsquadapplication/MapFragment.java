/**
 * Classname: AddMoodFragment
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// imports
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
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
import com.example.segfaultsquadapplication.MoodEvent;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import com.google.android.material.chip.ChipGroup;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import android.graphics.Color;
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
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private FirebaseFirestore db;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        db = FirebaseFirestore.getInstance();
        // init mood lists
        userMoods = new ArrayList<>();
        followedMoods = new HashMap<>();
        localMoods = new ArrayList<>();
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
                updateMapMarkers(TAB_MY_MOODS);
            } else if (checkedId == R.id.chip_followed_moods) {
                // Handle Followed Moods selection
                updateMapMarkers(TAB_FOLLOWED);
            } else if (checkedId == R.id.chip_local_moods) {
                // Handle Local Moods selection
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
            applyFilter("By Mood");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            applyFilter("By Reason");
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


/*
        // Example Firestore GeoPoint (latitude, longitude)
        com.google.firebase.firestore.GeoPoint firestoreGeoPoint = new com.google.firebase.firestore.GeoPoint(37.7749, -122.4194); // Example: San Francisco

        // Convert Firestore GeoPoint to OSMDroid GeoPoint
        org.osmdroid.util.GeoPoint osmGeoPoint = new org.osmdroid.util.GeoPoint(
                firestoreGeoPoint.getLatitude(),
                firestoreGeoPoint.getLongitude()
        );

        // Set map center to converted GeoPoint
        mapView.getController().setCenter(osmGeoPoint);


        // Enable location overlay
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);
*/
        // Add compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        return view;
    }



    private void loadMoodData() {
        String currentUserId = getCurrentUserId(); // TODO: Implement this method to get current user's ID

        // Load user's moods
        db.collection("moods")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(this::handleUserMoods);

        // Load followed users' moods
        loadFollowedUsersMoods();

        // Load local moods (if location available)
        if (currentLocation != null) {
            loadLocalMoods();
        }
    }

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

    private void loadFollowedUsersMoods() {
        // First get list of followed users
        String currentUserId = getCurrentUserId();
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> followedUsers = new ArrayList<>();
                    for (var doc : snapshot.getDocuments()) {
                        String followedId = doc.getString("followedId");
                        if (followedId != null) {
                            followedUsers.add(followedId);
                        }
                    }
                    // Then get their most recent moods
                    for (String userId : followedUsers) {
                        db.collection("moods")
                                .whereEqualTo("userId", userId)
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(moodSnapshot -> {
                                    if (!moodSnapshot.isEmpty()) {
                                        MoodEvent mood = moodSnapshot.getDocuments().get(0).toObject(MoodEvent.class);
                                        if (mood != null) {
                                            followedMoods.put(userId, mood);
                                            if (mapChipGroup.getCheckedChipId() == R.id.chip_followed_moods) {
                                                updateMapMarkers(TAB_FOLLOWED);
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    private void loadLocalMoods() {
        if (currentLocation == null || mapChipGroup == null)
            return;

        // Create a GeoPoint for the current location
        GeoPoint center = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        // Get all moods and filter by distance
        db.collection("moods")
                .get()
                .addOnSuccessListener(snapshot -> {
                    localMoods.clear();
                    for (var doc : snapshot.getDocuments()) {
                        MoodEvent mood = doc.toObject(MoodEvent.class);
                        if (mood != null && mood.getLocation() != null &&
                                isWithinRadius(mood.getLocation(), center, LOCAL_RADIUS_KM)) {
                            localMoods.add(mood);
                        }
                    }
                    if (mapChipGroup.getCheckedChipId() == R.id.chip_local_moods) {
                        updateMapMarkers(TAB_LOCAL);
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

                int color = mood.getPrimaryColor(requireContext());
                //mapView.addMarker(x, y, color, mood.getMoodType().toString());
            }
        }
    }

    // The method to get the current user's ID
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid(); // Return the unique ID of the current user
        } else {
            // No user is logged in
            return null;
        }
    }

    private void updateCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Exit if permission is not granted
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);

                        // Update map with user's location
                        updateMapLocation(latitude, longitude);
                    } else {
                        setDefaultLocation(); // Handle case where location is null
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Location", "Failed to get location", e);
                    setDefaultLocation(); // Handle failure case
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
     * Sets a default location (e.g., San Francisco) when location retrieval fails.
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
