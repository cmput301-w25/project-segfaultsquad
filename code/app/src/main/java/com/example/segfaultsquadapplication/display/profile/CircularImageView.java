package com.example.segfaultsquadapplication.display.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * This file is for user profile image with drawing and set images
 * user profile image from firebase cropped into circle
 */
public class CircularImageView extends AppCompatImageView {
    private Paint paint;
    private Bitmap bitmap;
    private BitmapShader shader;

    /**
     * initialize the view
     * @param context context of the application
     */
    public CircularImageView(Context context) {
        super(context);
        init();
    }

    /**
     * constructor to initialize XML attributes
     * @param context context of the application
     * @param attrs attribute set with XML parameters
     */
    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    /**
     * draws the circular image on the canvas
     * valid bitmap is set, it is drawn as a circle in the center of the view
     * @param canvas canvas to draw the circular image on.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, paint);
        } else {
            super.onDraw(canvas);
        }
    }

    /**
     * Sets a new bitmap image  refreshes view to display circle image
     * @param bm The new bitmap to be set.
     */
    @Override
    public void setImageBitmap(Bitmap bm) {
        bitmap = bm;
        super.setImageBitmap(bm);
    }
}
