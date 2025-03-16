package com.example.segfaultsquadapplication.impl.location;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LocationManager {
    private static final String LOG_TITLE = "LocationManager";
    private static Activity currActivity = null;
    private static FusedLocationProviderClient fusedLocationClient = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    /**
     * Prepares the location provider; do this during init of activities that need location info.
     * @param activity The activity
     */
    public static void prepareLocationProvider(Activity activity) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        if (! checkLocationPermission(activity) ) {
            requestLocationPermission(activity);
        }
        currActivity = activity;
    }
    public static void getLocation(AtomicReference<GeoPoint> holder, Consumer<Boolean> callback) {
        if (fusedLocationClient == null) {
            Log.d(LOG_TITLE, "Fused Location Client not initialized");
            callback.accept(false);
            return;
        }
        if (! checkLocationPermission(currActivity)) {
            Log.d(LOG_TITLE, "No permission to get location");
            callback.accept(false);
            return;
        }
        fusedLocationClient.getLastLocation()
                // Location retrieved
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d(LOG_TITLE, "Location retrieved");
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        holder.set(geoPoint);
                        callback.accept(true);
                    } else {
                        Log.d(LOG_TITLE, "Location is null");
                        callback.accept(false);
                    }
                })
                // Location not retrieved
                .addOnFailureListener(e -> {
                    Log.d(LOG_TITLE, "Location can not be retrieved");
                    callback.accept(false);
                });
    }
    /**
     * helper method to check if user gave permission for location use
     *
     * @param activity The activity
     * @return bool value of permission obtained or denied
     */
    private static boolean checkLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * helper method to obtain user permission for location permisssion
     *
     * @param activity The corresponding activity
     */
    private static void requestLocationPermission(Activity activity) {
        requestPermissions(activity,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

//    /**
//     * helper method to verify results of location permission request
//     *
//     * @param requestCode
//     * @param permissions
//     * @param grantResults
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            // permission gramted
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                getUserLocationAndSaveMood();
//            } else {
//                // TODO: ask if this is what we are actualy supposed to do or not...
//                // permission denied, save mood without location
//                createAndSaveMood(null);
//            }
//        }
//    }
}
