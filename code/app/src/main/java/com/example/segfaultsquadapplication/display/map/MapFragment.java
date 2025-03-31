package com.example.segfaultsquadapplication.display.map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.location.LocationManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.GeoPoint;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This fragment displays a map where users can view mood events associated with themselves and their followers. It handles location permissions, mood filtering, and mood event loading from Firestore.
 * Outstanding Issues: None
 */
public class MapFragment extends Fragment {
    // Static variable / presets.
    private static final float NEAR_MOOD_EVENT_METERS = 5000f;
    // Default filter - display all
    private static final Predicate<MoodEvent> FILTER_ALL = (moodEvent) -> true;
    // Filter - only events from last week
    private static final Predicate<MoodEvent> FILTER_LAST_WEEK = event -> {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -1); // Subtract one week
        Date lastWeekDate = calendar.getTime();
        return event.getTimestamp().toDate().after(lastWeekDate);
    };
    // Filter - only events of the type
    private static final Function<String, Predicate<MoodEvent>> FILTER_BY_TYPE = type -> (event -> event.getMoodType().toString().equals(type) );
    // Filter - only events of the reason
    private static final Function<String, Predicate<MoodEvent>> FILTER_BY_REASON = reason -> (event -> event.getReasonText().toLowerCase().contains(reason.toLowerCase()));
    // Filter - only events within 5 km
    private static final Function<Location, Predicate<MoodEvent>> FILTER_NEAR = currPos -> (event -> {
        Location moodLocation = new Location("");
        moodLocation.setLatitude(event.getLocation().getLatitude());
        moodLocation.setLongitude(event.getLocation().getLongitude());
        float distance = currPos.distanceTo(moodLocation);
        return distance <= NEAR_MOOD_EVENT_METERS;
    } );

    // Attributes
    private Location currentLocation;

    // Data for display
    private HashMap<String, User> userCache;
    private List<MoodEvent> userMoods; // The user's own mood events
    private List<MoodEvent> followedMoods; // The followed users' mood events

    private MapView mapView;

    // Add these as class members for filtering on map screen
    private ChipGroup mapChipGroup;
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private List<MoodEvent> displayedMoods = new ArrayList<>();
    private Map<String, Integer> locationOffsets = new HashMap<>();

    // Map display filters
    private Predicate<MoodEvent> filter = FILTER_ALL;

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
        // Init data containers
        userCache = new HashMap<>();
        userMoods = new ArrayList<>();
        followedMoods = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize filter views
        mapChipGroup = view.findViewById(R.id.map_chip_group);
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);

        // Setup filter button click listener
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        // Setup filter option click listeners
        wireFilterListeners(view);

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
     * Helper method to load current user's moods and followed user's moods.
     * By default, this will display all of the current user's moods.
     */
    private void loadMoodData() {
        String currentUserId = UserManager.getUserId();

        // Debugging log
        Log.d("MoodHistory", "Loading moods for user: " + currentUserId);

        // Load the user details first
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder, isSuccess -> {
            if (!isSuccess) {
                Log.e("MoodHistory", "Failed to load user data.");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // Cache the current user
            User currentUser = userHolder.get();
            userCache.put(currentUserId, currentUser);
            Log.d("MoodHistory", "Loaded user: " + currentUser.getUsername());

            // Now fetch the mood events for current user
            {
                ArrayList<MoodEvent> temp = new ArrayList<>();
                MoodEventManager.getAllMoodEvents(currentUserId, MoodEventManager.MoodEventFilter.ALL, temp,
                        moodLoadSuccess -> {
                            if (!moodLoadSuccess) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }

                            Log.d("MoodHistory", "Number of moods retrieved: " + temp.size());
                            clearDisplayedMoodEvents();
                            for (MoodEvent mood : temp) {
                                // Ignore mood events with no location info
                                if (mood.getLocation() == null) continue;

                                userMoods.add(mood); // Add to arraylist
                                Log.d("MoodHistory",
                                        "Loaded mood: " + mood.getMoodType() + " with ID: " + mood.getDbFileId());

                                // Add each mood as a marker on the map
                                if (mood.getLocation() != null) {
                                    addMoodMarkerToMap(mood);
                                }
                            }
                        });
            }

            // Now fetch the mood events & user info for followed users
            for (String followedUserId : currentUser.getFollowing()) {
                // Load user info
                AtomicReference<User> followedUserHolder = new AtomicReference<>();
                UserManager.loadUserData(followedUserId, followedUserHolder, flwUserLoadSucc -> {
                    if (!flwUserLoadSucc) {
                        Log.e("MoodHistory", "Failed to load user data for id " + followedUserId);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        userCache.put(followedUserId, followedUserHolder.get());
                    }
                });

                // Load events info
                ArrayList<MoodEvent> temp = new ArrayList<>();
                MoodEventManager.getAllMoodEvents(followedUserId, MoodEventManager.MoodEventFilter.PUBLIC_ONLY, temp,
                        moodLoadSuccess -> {
                            if (!moodLoadSuccess) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }

                            Log.d("MoodHistory", "Number of moods retrieved: " + temp.size());

                            for (MoodEvent mood : temp) {
                                // Ignore mood events with no location info
                                if (mood.getLocation() == null) continue;

                                followedMoods.add(mood); // Add to arraylist
                                Log.d("MoodHistory",
                                        "Loaded mood: " + mood.getMoodType() + " with ID: " + mood.getDbFileId());
                            }
                        });
            }
        });
    }

    private void wireFilterListeners(View view) {
        // Chip group
        mapChipGroup.setOnCheckedStateChangeListener( (group, checkedIds) -> {
            // When changing between data groups, reset the filter.
            filter = FILTER_ALL;
            onNewFilterApplied();
        });
        // Filter tabs
        view.findViewById(R.id.filter1).setOnClickListener(v -> {
            filter = FILTER_LAST_WEEK;
            toggleFilterMenu();
            onNewFilterApplied();
        });

        view.findViewById(R.id.filter2).setOnClickListener(v -> {
            showMoodFilterDialog();
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            showReasonFilterDialog();
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter4).setOnClickListener(v -> {
            if (currentLocation == null) {
                Log.e("Location", "Current location is not available");
            } else {
                filter = FILTER_NEAR.apply(currentLocation);
                toggleFilterMenu();
                onNewFilterApplied();
            }
        });

        view.findViewById(R.id.clearFilters).setOnClickListener(v -> {
            filter = FILTER_ALL;
            toggleFilterMenu();
            onNewFilterApplied();
        });
    }

    /**
     * the dialog fragment for the filter functionality for filtering by specified
     * mood
     */
    private void showMoodFilterDialog() {
        String[] moods = MoodEvent.MoodType.getAllTypeNames();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Mood")
                .setItems(moods, (dialog, which) -> {
                    String selectedMood = moods[which];
                    filter = FILTER_BY_TYPE.apply(selectedMood);
                    onNewFilterApplied();
                });
        builder.show();
    }

    /**
     * the dialog fragment for the filter functionality for filtering by specified
     * mood
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
        // Setting margins programmatically for the input field to match the title's
        // indent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 0, 40, 0);
        input.setLayoutParams(params);
        layout.addView(input); // add the EditText tot he layout
        builder.setView(layout); // set the layout as the dialog

        builder.setPositiveButton("OK", (dialog, which) -> {
            String keyword = input.getText().toString();
            filter = FILTER_BY_REASON.apply(keyword);
            onNewFilterApplied();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * When a new filter is applied, clear the screen and display relevant mood events.
     */
    private void onNewFilterApplied() {
        List<MoodEvent> src = mapChipGroup.getCheckedChipId() == R.id.chip_my_moods ? userMoods : followedMoods;
        // Display the new events.
        clearDisplayedMoodEvents();
        for (MoodEvent evt : src) {
            if (filter.test(evt)) {
                addMoodMarkerToMap(evt);
            }
        }
    }

    /**
     * adds the mood event markers to the map
     * 
     * @param mood
     *             the mood event to mark
     */
    private void addMoodMarkerToMap(MoodEvent mood) {
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
        String userName = "Unknown";
        if (userCache.containsKey(mood.getUserId())) {
            userName = userCache.get(mood.getUserId()).getUsername();
        }
        marker.setTitle(userName + ": " + mood.getMoodType());

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
     * Clears the previously displayed mood events and their consequences,
     * e.g. the "mood count" around the same position.
     */
    private void clearDisplayedMoodEvents() {
        // Clear markers and registered moods
        clearMoodMarkers();
        displayedMoods.clear();
        // Clear local mood counts
        locationOffsets.clear();
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
        displayedMoods.clear();
    }
}