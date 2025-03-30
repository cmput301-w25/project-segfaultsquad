package com.example.segfaultsquadapplication.display.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class CircularImageView extends AppCompatImageView {
    private Paint paint;
    private Bitmap bitmap;
    private BitmapShader shader;

    public CircularImageView(Context context) {
        super(context);
        init();
    }

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

    @Override
    public void setImageBitmap(Bitmap bm) {
        bitmap = bm;
        super.setImageBitmap(bm);
    }
}
