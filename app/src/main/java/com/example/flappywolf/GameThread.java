package com.example.flappywolf;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
    private final SurfaceHolder holder;
    private final GameView view;
    private boolean running = false;
    private static final long FRAME_TIME = 1000 / 60; // ~60 FPS

    public GameThread(SurfaceHolder h, GameView v) {
        holder = h;
        view   = v;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (running) {
            long start = System.currentTimeMillis();
            // 1) Update game logic
            view.update();

            // 2) Render
            Canvas canvas = null;
            try {
                // If the surface has been destroyed, stop the loop
                if (!holder.getSurface().isValid()) break;

                canvas = holder.lockCanvas();
                if (canvas != null) {
                    view.draw(canvas);
                }
            } catch (IllegalStateException e) {
                // Surface was released unexpectedly
                break;
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (IllegalStateException ignored) {
                        // Ignore if it fails
                    }
                }
            }

            // 3) Cap frame rate
            long delta = System.currentTimeMillis() - start;
            long sleep = FRAME_TIME - delta;
            if (sleep > 0) {
                try { Thread.sleep(sleep); }
                catch (InterruptedException ignored) {}
            }
        }
    }
}