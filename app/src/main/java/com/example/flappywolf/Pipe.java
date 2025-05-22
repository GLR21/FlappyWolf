package com.example.flappywolf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

public class Pipe {
    public static Bitmap RAW_HEAD, RAW_HEAD_FLIPPED, RAW_BODY;
    public static final float SPEED = 8f;

    private float x, top, totalHeight;
    private final boolean isTop;
    private boolean scored = false;
    private final float widthPx, headH;

    public Pipe(float centerY, float widthPx, float gapPx,
                boolean isTopPipe, float screenH, float screenW) {
        this.widthPx = widthPx;
        float aspect = RAW_HEAD.getHeight() / (float)RAW_HEAD.getWidth();
        headH = widthPx * aspect;

        isTop = isTopPipe;
        float halfGap = gapPx * 0.5f;
        if (isTop) {
            top = 0;
            totalHeight = centerY - halfGap;
        } else {
            top = centerY + halfGap;
            totalHeight = screenH - top;
        }

        x = screenW;
    }

    public void update() {
        x -= SPEED;
    }

    public void draw(Canvas c) {
        float bodyH = totalHeight - headH;
        RectF bodyDst, headDst;

        if (isTop) {
            bodyDst = new RectF(x, 0, x + widthPx, bodyH);
            headDst = new RectF(x, bodyH, x + widthPx, bodyH + headH);
        } else {
            headDst = new RectF(x, top, x + widthPx, top + headH);
            bodyDst = new RectF(x, top + headH, x + widthPx, top + totalHeight);
        }

        c.drawBitmap(RAW_BODY, null, bodyDst, null);
        c.drawBitmap(isTop ? RAW_HEAD_FLIPPED : RAW_HEAD, null, headDst, null);
    }

    public float getX()         { return x; }
    public float getWidth()     { return widthPx; }
    public boolean isTop()      { return isTop; }
    public boolean isScored()   { return scored; }
    public void setScored(boolean s) { scored = s; }

    public boolean collidesWith(Bird b) {
        float cx = b.getX() + b.getWidth()*0.5f;
        float cy = b.getY() + b.getHeight()*0.5f;
        float r  = b.getWidth()*0.5f*0.6f;

        float left = x, right = x + widthPx;
        float topE = isTop?0:top, botE = isTop?totalHeight:(top+totalHeight);

        float nx = Math.max(left,   Math.min(cx, right));
        float ny = Math.max(topE,   Math.min(cy,   botE));
        float dx = cx - nx, dy = cy - ny;
        return dx*dx + dy*dy < r*r;
    }
}