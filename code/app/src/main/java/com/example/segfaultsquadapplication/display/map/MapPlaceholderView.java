/**
 * Classname: MapPlaceholderView
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This custom view serves as a placeholder for the map, allowing for the
 * visualization of mood markers. It draws a grid and mood markers on the canvas.
 *
 * Outstanding Issues: None
 */

package com.example.segfaultsquadapplication.display.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * This custom view serves as a placeholder for the map, allowing for the visualization of mood markers. It draws a grid and mood markers on the canvas.
 * Outstanding Issues: None
 */
public class MapPlaceholderView extends View {
    private Paint backgroundPaint;
    private Paint markerPaint;
    private Paint textPaint;
    private List<MoodMarker> markers;

    /**
     * Class representing a mood marker on the map.
     */
    public static class MoodMarker {
        float x, y;
        int color;
        String label;

        public MoodMarker(float x, float y, int color, String label) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.label = label;
        }
    }

    public MapPlaceholderView(Context context) {
        super(context);
        init();
    }

    public MapPlaceholderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);

        markerPaint = new Paint();
        markerPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        markers = new ArrayList<>();
    }

    /**
     * Clears all markers from the view.
     */
    public void clearMarkers() {
        markers.clear();
        invalidate();
    }

    /**
     * Adds a marker to the view.
     *
     * @param x     The x-coordinate of the marker.
     * @param y     The y-coordinate of the marker.
     * @param color The color of the marker.
     * @param label The label for the marker.
     */
    public void addMarker(float x, float y, int color, String label) {
        markers.add(new MoodMarker(x, y, color, label));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // Draw grid lines
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStrokeWidth(2);

        // Draw grid
        for (int i = 0; i < getWidth(); i += 50) {
            canvas.drawLine(i, 0, i, getHeight(), gridPaint);
        }
        for (int i = 0; i < getHeight(); i += 50) {
            canvas.drawLine(0, i, getWidth(), i, gridPaint);
        }

        // Draw markers
        for (MoodMarker marker : markers) {
            markerPaint.setColor(marker.color);
            canvas.drawCircle(marker.x * getWidth(), marker.y * getHeight(), 20, markerPaint);
            canvas.drawText(marker.label, marker.x * getWidth(), marker.y * getHeight() - 30, textPaint);
        }

        // Draw "Map Placeholder" text
        textPaint.setTextSize(50f);
        canvas.drawText("Map Placeholder", getWidth() / 2f, 50, textPaint);
    }
}