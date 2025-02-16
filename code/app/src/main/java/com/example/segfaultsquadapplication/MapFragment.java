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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import android.graphics.Color;

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

    private MapPlaceholderView mapView;

    // permissions handling
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                }
            });

    private ChipGroup mapChipGroup;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapChipGroup = view.findViewById(R.id.map_chip_group);

        // Set default selection
        mapChipGroup.check(R.id.chip_my_moods);

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

        setupPlaceholderMap(view);
        return view;
    }

    private void setupPlaceholderMap(View view) {
        mapView = view.findViewById(R.id.map_placeholder);
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

    private boolean isWithinRadius(GeoPoint point1, GeoPoint point2, float radiusKm) {
        float[] results = new float[1];
        Location.distanceBetween(
                point1.getLatitude(), point1.getLongitude(),
                point2.getLatitude(), point2.getLongitude(),
                results);
        return results[0] <= radiusKm * 1000; // Convert km to meters
    }

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

                int color = getMoodColor(mood.getMoodType());
                mapView.addMarker(x, y, color, mood.getMoodType().toString());
            }
        }
    }

    private int getMoodColor(MoodEvent.MoodType moodType) {
        switch (moodType) {
            case HAPPY:
                return Color.GREEN;
            case SAD:
                return Color.BLUE;
            case ANGRY:
                return Color.RED;
            case EXCITED:
                return Color.YELLOW;
            case TIRED:
                return Color.rgb(255, 165, 0); // Orange
            case SCARED:
                return Color.rgb(148, 0, 211); // Violet
            case SURPRISED:
                return Color.CYAN;
            default:
                return Color.GRAY;
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
}
