package com.example.flappywolf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread;
    private Bitmap background, modalBg, modalBtn;
    private Bird bird;
    private List<Pipe> pipes;
    private List<Coin> coins;
    private int score, bestScore;
    private SharedPreferences prefs;
    private GameState state;

    private Paint textPaint;
    private RectF modalRect, btnRestartRect;
    private Random random = new Random();

    private static final float PIPE_WIDTH_FACTOR = 5f;
    private static final float PIPE_GAP_FACTOR   = 4f;
    private static final float PIPE_SPACING      = 600f;
    private static final float SPEED             = 8f;  // must match Pipe.SPEED

    // accumulates world scroll for spawn timing
    private float spawnTimer = 0f;

    private enum GameState { START, PLAYING, FALLING, GAMEOVER }

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE);
        loadAssets();
        initGame();
        initTextPaint();
    }

    private void loadAssets() {
        BitmapHelper helper = new BitmapHelper(getContext());
        background = helper.loadFullScreen(R.drawable.background);
        modalBg    = BitmapFactory.decodeResource(getResources(), R.drawable.modal_background);
        modalBtn   = BitmapFactory.decodeResource(getResources(), R.drawable.modal_button);

        // pre-load Pipe bitmaps
        Pipe.RAW_HEAD  = BitmapFactory.decodeResource(getResources(), R.drawable.pipe_head);
        Pipe.RAW_BODY  = BitmapFactory.decodeResource(getResources(), R.drawable.pipe_body);
        Matrix flip    = new Matrix();
        flip.preScale(1f, -1f);
        Pipe.RAW_HEAD_FLIPPED = Bitmap.createBitmap(
                Pipe.RAW_HEAD, 0, 0,
                Pipe.RAW_HEAD.getWidth(), Pipe.RAW_HEAD.getHeight(),
                flip, true
        );

        // pre-load Coin bitmaps
        Coin.RAW_FRONT = BitmapFactory.decodeResource(getResources(), R.drawable.coin_front);
        Coin.RAW_SIDE  = BitmapFactory.decodeResource(getResources(), R.drawable.coin_side);
    }

    private void initGame() {
        BitmapHelper helper = new BitmapHelper(getContext());
        bird       = new Bird(getContext(), helper);
        pipes      = new ArrayList<>();
        coins      = new ArrayList<>();
        score      = 0;
        bestScore  = prefs.getInt("best_score", 0);
        state      = GameState.START;

        // reset spawn timer
        spawnTimer = 0f;

        // initial preview spawn
        float w     = helper.screenWidth;
        float h     = helper.screenHeight;
        float gap   = h / PIPE_GAP_FACTOR;
        float cY    = h * 0.5f;
        float pW    = w / PIPE_WIDTH_FACTOR;
        float spawn = w + pW * 1.5f;

        pipes.add(new Pipe(cY, pW, gap, true,  h, w));
        pipes.add(new Pipe(cY, pW, gap, false, h, w));
        coins.add(new Coin(spawn + pW/2f, cY, pW * 0.6f));
    }

    private void initTextPaint() {
        Typeface font = ResourcesCompat.getFont(getContext(), R.font.press_start_2p);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(64);
        textPaint.setTypeface(font);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        initModalLayout();
        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }
    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int h2){}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        thread.setRunning(false);
        try { thread.join(); } catch (InterruptedException ignored) {}
        thread = null;
    }

    private void initModalLayout() {
        float sw = getWidth(), sh = getHeight();
        float panelW = sw * 0.8f;
        float aspect = modalBg.getHeight() / (float)modalBg.getWidth();
        float panelH = panelW * aspect;
        float left   = (sw - panelW)/2f, top = (sh - panelH)/2f;
        modalRect    = new RectF(left, top, left + panelW, top + panelH);

        float btnW   = panelW * 0.6f;
        float btnAsp = modalBtn.getHeight() / (float)modalBtn.getWidth();
        float btnH   = btnW * btnAsp;
        float bx     = modalRect.centerX() - btnW/2f;
        float by     = modalRect.bottom - btnH - panelH*0.1f;
        btnRestartRect = new RectF(bx, by, bx + btnW, by + btnH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return super.onTouchEvent(e);
        float x = e.getX(), y = e.getY();

        if (state == GameState.START) {
            state = GameState.PLAYING;
        }
        else if (state == GameState.PLAYING) {
            bird.jump();
        }
        else if (state == GameState.FALLING) {
            // ignore taps while falling
        }
        else { // GAMEOVER
            if (btnRestartRect.contains(x,y)) initGame();
        }
        return true;
    }

    public void update() {
        if (state == GameState.FALLING) {
            bird.update();
            float floor = getHeight() - bird.getHeight();
            if (bird.getY() >= floor) {
                bird.setY(floor);
                if (score > bestScore) {
                    bestScore = score;
                    prefs.edit().putInt("best_score",bestScore).apply();
                }
                state = GameState.GAMEOVER;
            }
            return;
        }
        if (state != GameState.PLAYING) return;

        // bird physics
        bird.update();

        // spawn new pipes/coins every PIPE_SPACING px of world scroll
        spawnTimer += SPEED;
        if (spawnTimer >= PIPE_SPACING) {
            spawnTimer -= PIPE_SPACING;
            float sw = getWidth(), sh = getHeight();
            float gap = sh / PIPE_GAP_FACTOR;
            float cY  = sh * (0.3f + random.nextFloat()*0.4f);
            float pW  = sw / PIPE_WIDTH_FACTOR;
            float startX = sw;

            pipes.add(new Pipe(cY, pW, gap, true,  sh, sw));
            pipes.add(new Pipe(cY, pW, gap, false, sh, sw));
            coins.add(new Coin(startX + pW/2f, cY , pW * 0.6f));
        }

        // update & collision for pipes
        for (Iterator<Pipe> it = pipes.iterator(); it.hasNext(); ) {
            Pipe p = it.next();
            p.update();
            if (p.getX() + p.getWidth() < 0) { it.remove(); continue; }

            if (p.collidesWith(bird)) {
                bird.die();
                state = GameState.FALLING;
            }
            // scoring for passing bottom pipe
            if (!p.isTop() && !p.isScored() && p.getX() + p.getWidth() < bird.getX()) {
                score++;
                p.setScored(true);
            }
        }

        // update & collection for coins
        for (Iterator<Coin> it = coins.iterator(); it.hasNext(); ) {
            Coin c = it.next();
            c.update();
            if (c.getX() + c.getWidth() < 0) { it.remove(); continue; }
            if (c.collidesWith(bird)) {
                score++;
                it.remove();
            }
        }

        // bounds checks
        if (bird.getY() < 0) {
            bird.setY(0);
            bird.die();
            state = GameState.FALLING;
        }
        if (bird.getY() + bird.getHeight() > getHeight()) {
            bird.setY(getHeight() - bird.getHeight());
            if (score > bestScore) {
                bestScore = score;
                prefs.edit().putInt("best_score",bestScore).apply();
            }
            state = GameState.GAMEOVER;
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
        if (canvas == null) return;

        canvas.drawBitmap(background, 0, 0, null);

        // CME-safe iteration
        for (Pipe p : new ArrayList<>(pipes)) p.draw(canvas);
        for (Coin c : new ArrayList<>(coins)) c.draw(canvas);

        bird.draw(canvas);

        // HUD score
        if (state != GameState.START) {
            String s = String.valueOf(score);
            float sw = textPaint.measureText(s), cx = getWidth()/2f, sy = 150f;
            textPaint.setStyle(Paint.Style.STROKE);
            textPaint.setStrokeWidth(8);
            textPaint.setColor(0xFF000000);
            canvas.drawText(s, cx-sw/2f, sy, textPaint);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(0xFFFFFFFF);
            canvas.drawText(s, cx-sw/2f, sy, textPaint);
        }

        if (state == GameState.START) {
            textPaint.setTextSize(48);
            drawOutlinedText(canvas, "Tap to Start", getWidth()/2f, getHeight()/2f);
        }
        else if (state == GameState.GAMEOVER) {
            canvas.drawColor(0x88000000);
            canvas.drawBitmap(modalBg, null, modalRect, null);
            float cx = modalRect.centerX(), ph = modalRect.height(), gapPx = 20f;

            textPaint.setTextSize(48);
            drawOutlinedText(canvas, "SCORE", cx, modalRect.top + ph*0.20f);
            textPaint.setTextSize(96);
            drawOutlinedText(canvas, String.valueOf(score),
                    cx, modalRect.top + ph*0.20f + textPaint.getTextSize() + gapPx);
            textPaint.setTextSize(48);
            drawOutlinedText(canvas, "BEST", cx, modalRect.top + ph*0.45f);
            textPaint.setTextSize(96);
            drawOutlinedText(canvas, String.valueOf(bestScore),
                    cx, modalRect.top + ph*0.45f + textPaint.getTextSize() + gapPx);

            canvas.drawBitmap(modalBtn, null, btnRestartRect, null);
            textPaint.setTextSize(48);
            drawOutlinedText(canvas, "RESTART",
                    btnRestartRect.centerX(), btnRestartRect.centerY() + 16f);
        }
    }

    private void drawOutlinedText(Canvas c, String txt, float cx, float y) {
        float w = textPaint.measureText(txt);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(8);
        textPaint.setColor(0xFF000000);
        c.drawText(txt, cx - w/2f, y, textPaint);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFFFFFFFF);
        c.drawText(txt, cx - w/2f, y, textPaint);
    }

    public void pause()  { if (thread != null) thread.setRunning(false); }
    public void resume() {
        if (thread == null || !thread.isRunning()) {
            thread = new GameThread(getHolder(), this);
            thread.setRunning(true);
            thread.start();
        }
    }
}