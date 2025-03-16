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

public class Map_api {
    public interface GeocodingListener {
        void onLocationFound(double latitude, double longitude);
        void onError(String error);
    }

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
}
