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
import com.google.firebase.firestore.GeoPoint;
import com.example.segfaultsquadapplication.MoodEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class MapFragment extends Fragment {
    private GoogleMap mMap;
    private TabLayout tabLayout;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    private List<MoodEvent> myMoods;
    private List<MoodEvent> followedMoods;
    private List<MoodEvent> localMoods;

    private static final int TAB_MY_HISTORY = 0;
    private static final int TAB_FOLLOWED = 1;
    private static final int TAB_LOCAL = 2;


    // permissions handling
    private final ActivityResultLauncher<String> requestPermissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            enableMyLocation();
        }
    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);


        initializeViews(view);          // Initialize UI components
        // TODO: comment this back in once the google maps api stuff is figured out
        // setupMap();                     // Setup map
        loadDummyData();                // Load dummy data

        return view;
    }

    private void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);

        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("My Mood History"));
        tabLayout.addTab(tabLayout.newTab().setText("Followed Moods"));
        tabLayout.addTab(tabLayout.newTab().setText("Local Moods"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateMapMarkers(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            // Enable my location button if permission is granted
            enableMyLocation();
            // Set initial camera position to Edmonton
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(53.5461, -113.4937), 12));
            // Show initial tab's markers
            updateMapMarkers(TAB_MY_HISTORY);
        });
    }

    private Location getCurrentLocation() {
        return currentLocation;
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
                    updateMapMarkers(tabLayout.getSelectedTabPosition());
                }
            });
    }

    private void enableMyLocation() {
        if (mMap == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            updateCurrentLocation();
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
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
                .setMessage("The app needs location permission to show nearby moods and attach location to your mood events.")
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

    private void loadDummyData() {
        myMoods = new ArrayList<>();
        followedMoods = new ArrayList<>();
        localMoods = new ArrayList<>();

        // Add dummy mood events for current user (My Mood History)
        myMoods.add(createDummyMood(53.5461, -113.4937, MoodEvent.MoodType.HAPPY, "Me", "At the park"));
        myMoods.add(createDummyMood(53.5433, -113.4947, MoodEvent.MoodType.EXCITED, "Me", "Shopping"));

        // Add dummy mood events for followed users
        followedMoods.add(createDummyMood(53.5451, -113.4957, MoodEvent.MoodType.HAPPY, "John", "Coffee"));
        followedMoods.add(createDummyMood(53.5441, -113.4967, MoodEvent.MoodType.SAD, "Alice", "Rain"));
        followedMoods.add(createDummyMood(53.5481, -113.4917, MoodEvent.MoodType.ANGRY, "Bob", "Traffic"));

        // Add dummy local mood events (within 5km)
        localMoods.add(createDummyMood(53.5471, -113.4927, MoodEvent.MoodType.HAPPY, "Local1", "Park"));
        localMoods.add(createDummyMood(53.5465, -113.4940, MoodEvent.MoodType.EXCITED, "Local2", "Mall"));
    }

    private MoodEvent createDummyMood(double lat, double lng, MoodEvent.MoodType moodType,
                                      String username, String reason) {
        MoodEvent mood = new MoodEvent(username, moodType, reason);
        mood.setLocation(new GeoPoint(lat, lng));
        mood.setTimestamp(new Date());
        return mood;
    }

    private void updateMapMarkers(int tabPosition) {
        if (mMap == null) return;

        mMap.clear();
        List<MoodEvent> moodsToShow;

        switch (tabPosition) {
            case TAB_MY_HISTORY:
                moodsToShow = myMoods;
                break;
            case TAB_FOLLOWED:
                moodsToShow = followedMoods;
                break;
            case TAB_LOCAL:
                moodsToShow = localMoods;
                break;
            default:
                return;
        }

        for (MoodEvent mood : moodsToShow) {
            // Skip if mood is outside 5km radius (for local moods)
            if (tabPosition == TAB_LOCAL && !isWithin5Km(mood.getLocation())) {
                continue;
            }

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(
                            mood.getLocation().getLatitude(),
                            mood.getLocation().getLongitude()))
                    .title(mood.getUserId())
                    .snippet(mood.getReasonText());

            // Set marker color based on mood type
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(getMoodColor(mood.getMoodType())));
            mMap.addMarker(markerOptions);
        }
    }

    private float getMoodColor(MoodEvent.MoodType moodType) {
        switch (moodType) {
            case HAPPY: return BitmapDescriptorFactory.HUE_GREEN;
            case SAD: return BitmapDescriptorFactory.HUE_BLUE;
            case ANGRY: return BitmapDescriptorFactory.HUE_RED;
            case EXCITED: return BitmapDescriptorFactory.HUE_YELLOW;
            default: return BitmapDescriptorFactory.HUE_AZURE;
        }
    }

    private boolean isWithin5Km(GeoPoint location) {
        // Get current location
        Location currentLocation = getCurrentLocation();
        if (currentLocation == null) return true; // Show all if location unavailable

        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                location.getLatitude(), location.getLongitude(),
                results);

        return results[0] <= 5000; // 5000 meters = 5km
    }



    @Override
    public void onResume() {
        super.onResume();
        updateCurrentLocation();
    }
}
