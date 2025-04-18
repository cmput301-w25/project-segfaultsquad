package com.example.segfaultsquadapplication;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class interacts with OpenStreetMap API for geolocation coding (geocoding and reverse geocoding)
 */
public class Map_api {
    /**
     * Listener interface for receiving the results of a geocoding request.
     */
    public interface GeocodingListener {
        void onLocationFound(double latitude, double longitude);
        void onError(String error);
    }

    /**
     * Fetches  coordinates (latitude and longitude) for given address using geocoding API
     * @param address address to search for
     * @param listener listener to handle the result or error.
     */
    public static void getCoordinates(String address, GeocodingListener listener) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String encodedAddress = URLEncoder.encode(address, "UTF-8");
                String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedAddress;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(result.toString());
                if (jsonArray.length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");

                    // Post result to UI thread
                    handler.post(() -> listener.onLocationFound(lat, lon));
                } else {
                    handler.post(() -> listener.onError("Location not found"));
                }
            } catch (Exception e) {
                handler.post(() -> listener.onError("Failed to fetch data: " + e.getMessage()));
            }
        });
    }

    /**
     * Listener interface for receiving the results of a reverse geocoding request.
     */
    public interface ReverseGeocodingListener {
        void onAddressFound(String address);
        void onError(String error);
    }

    /**
     * Fetches address for the given coordinates (latitude and longitude) using reverse geocoding API.
     * @param latitude Latitude of location
     * @param longitude Longitude of location
     * @param listener listener to handle the result or error.
     */
    public static void getAddress(double latitude, double longitude, ReverseGeocodingListener listener) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + latitude + "&lon=" + longitude;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(result.toString());
                if (jsonObject.has("display_name")) {
                    String address = jsonObject.getString("display_name");
                    handler.post(() -> listener.onAddressFound(address));
                } else {
                    handler.post(() -> listener.onError("Address not found"));
                }
            } catch (Exception e) {
                handler.post(() -> listener.onError("Failed to fetch data: " + e.getMessage()));
            }
        });
    }
}