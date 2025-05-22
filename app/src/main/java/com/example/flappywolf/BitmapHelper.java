package com.example.flappywolf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class BitmapHelper {
    private final Context context;
    public final int screenWidth;
    public final int screenHeight;

    public BitmapHelper(Context context) {
        this.context = context;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    /**
     * Scales a drawable so its width = desiredWidthPx, preserving aspect.
     * Falls back gracefully if the resource bounds are missing.
     */
    public Bitmap loadScaledWidth(int resId, int desiredWidthPx) {
        // 1) Decode bounds only
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, opts);
        int origW = opts.outWidth;
        int origH = opts.outHeight;

        // 2) Fallback if bounds invalid
        if (origW <= 0 || origH <= 0) {
            Bitmap full = BitmapFactory.decodeResource(context.getResources(), resId);
            if (full == null) return null;
            int fallbackH = (int)((float)full.getHeight() * desiredWidthPx / full.getWidth());
            return Bitmap.createScaledBitmap(full, desiredWidthPx, fallbackH, true);
        }

        // 3) Compute target height
        int targetH = desiredWidthPx * origH / origW;

        // 4) Calculate sample size safely
        opts.inSampleSize = calculateInSampleSize(opts, desiredWidthPx, targetH);
        opts.inJustDecodeBounds = false;

        // 5) Decode subsampled
        Bitmap tmp = BitmapFactory.decodeResource(context.getResources(), resId, opts);
        if (tmp == null) {
            tmp = BitmapFactory.decodeResource(context.getResources(), resId);
            if (tmp == null) return null;
        }

        // 6) Final exact scale
        int scaledH = desiredWidthPx * tmp.getHeight() / tmp.getWidth();
        return Bitmap.createScaledBitmap(tmp, desiredWidthPx, scaledH, true);
    }

    /**
     * Stretches a drawable to fill the entire screen.
     */
    public Bitmap loadFullScreen(int resId) {
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        if (raw == null) return null;
        return Bitmap.createScaledBitmap(raw, screenWidth, screenHeight, true);
    }

    /**
     * Power-of-two sample size calculation, returns 1 if
     * any dimension is zero (avoiding divide-by-zero).
     */
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width  = options.outWidth;
        if (height <= 0 || width <= 0) return 1;

        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfH = height / 2;
            final int halfW = width  / 2;
            while ((halfH / inSampleSize) >= reqHeight
                    && (halfW / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}