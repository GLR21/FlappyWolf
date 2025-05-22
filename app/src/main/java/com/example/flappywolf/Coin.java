package com.example.flappywolf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

public class Coin {
    public static Bitmap RAW_FRONT, RAW_SIDE;
    private static final float SPEED = 8f;
    private float x, y, size;
    private int frame = 0;
    private static final int DELAY = 20;  // frames per side

    public Coin(float startX, float centerY, float displaySize) {
        x = startX;
        size = displaySize;
        y = centerY - size / 2f;
    }

    public void update() {
        x -= SPEED;
        // advance frame and wrap every 2*DELAY
        frame = (frame + 1) % (DELAY * 2);
    }

    public void draw(Canvas c) {
        // choose front for first DELAY frames, side for next DELAY
        boolean front = frame < DELAY;
        RectF dst = new RectF(x, y, x + size, y + size);
        c.drawBitmap(front ? RAW_FRONT : RAW_SIDE, null, dst, null);
    }

    public float getX()      { return x; }
    public float getWidth()  { return size; }
    public float getHeight() { return size; }

    public boolean collidesWith(Bird b) {
        float bx = b.getX(), by = b.getY();
        return bx + b.getWidth() > x && bx < x + size
                && by + b.getHeight() > y && by < y + size;
    }
}