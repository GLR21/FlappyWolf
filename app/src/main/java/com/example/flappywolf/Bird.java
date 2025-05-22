package com.example.flappywolf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Bird {
    private enum State { NEUTRAL, HAPPY, HURT }
    private Bitmap neutral, happy, hurt;
    private float x, y, v;
    private State state = State.NEUTRAL;
    private int frameCount = 0;
    private static final int JUMP_FRAMES = 5;

    public Bird(Context ctx, BitmapHelper h) {
        int w = h.screenWidth / 8;
        neutral = h.loadScaledWidth(R.drawable.bird_neutral, w);
        happy   = h.loadScaledWidth(R.drawable.bird_happy, w);
        hurt    = h.loadScaledWidth(R.drawable.bird_hurt, w);
        x = h.screenWidth * 0.2f;
        y = h.screenHeight * 0.5f;
        v = 0;
    }

    public void update() {
        v += 1f;
        y += v;
        if (state == State.HAPPY) {
            if (++frameCount > JUMP_FRAMES) { state = State.NEUTRAL; frameCount = 0; }
        }
    }

    public void jump() {
        v = -20f;
        state = State.HAPPY;
        frameCount = 0;
    }

    public void die() {
        state = State.HURT;
    }

    public void draw(Canvas c) {
        Bitmap b;
        switch (state) {
            case HAPPY: b = happy; break;
            case HURT:  b = hurt;  break;
            default:    b = neutral;
        }
        c.drawBitmap(b, x, y, null);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public void setY( float newY ) { this.y = newY; }
    public int getWidth() { return neutral.getWidth(); }
    public int getHeight() { return neutral.getHeight(); }
}
