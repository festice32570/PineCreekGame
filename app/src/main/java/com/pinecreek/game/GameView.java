package com.pinecreek.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Thread gameThread;
    private volatile boolean running = false;

    private float speed = 0f;
    private float roadOffset = 0f;
    private float distance = 0f;
    private boolean left, right, accelerate, brake;
    private long lastTime;

    private int mission = 0;
    private String message = "ミッション：町の食堂まで荷物を届けよう";

    private final RectF leftBtn = new RectF();
    private final RectF rightBtn = new RectF();
    private final RectF accelBtn = new RectF();
    private final RectF brakeBtn = new RectF();

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        resumeGame();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        pauseGame();
    }

    public synchronized void resumeGame() {
        if (running || !holder.getSurface().isValid()) return;
        running = true;
        lastTime = System.nanoTime();
        gameThread = new Thread(this, "PineCreekGameLoop");
        gameThread.start();
    }

    public synchronized void pauseGame() {
        running = false;
        if (gameThread != null && gameThread != Thread.currentThread()) {
            try {
                gameThread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        gameThread = null;
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - lastTime) / 1_000_000_000f);
            lastTime = now;
            update(dt);
            drawFrame();
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void update(float dt) {
        if (accelerate) speed += 22f * dt;
        else speed -= 5f * dt;
        if (brake) speed -= 30f * dt;
        speed = clamp(speed, 0f, 45f);

        float steer = 0f;
        if (left) steer -= 1f;
        if (right) steer += 1f;
        roadOffset += steer * (85f + speed * 2.2f) * dt;
        roadOffset *= (float)Math.pow(0.72, dt);
        roadOffset = clamp(roadOffset, -300f, 300f);

        distance += speed * dt;
        if (mission == 0 && distance > 500f) {
            mission = 1;
            message = "到着！ 次のミッション：町役場の前に行こう";
        } else if (mission == 1 && distance > 1050f) {
            mission = 2;
            message = "町役場に到着。雪に埋まった車を発見！";
        } else if (mission == 2 && distance > 1550f) {
            mission = 3;
            message = "ミッションクリア！ Pine Creekを自由に走ろう";
        }
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas c = holder.lockCanvas();
        if (c == null) return;
        try {
            final int w = c.getWidth();
            final int h = c.getHeight();
            drawWorld(c, w, h);
            drawHud(c, w, h);
        } finally {
            holder.unlockCanvasAndPost(c);
        }
    }

    private void drawWorld(Canvas c, int w, int h) {
        float horizon = h * 0.34f;

        // Winter sky
        c.drawColor(Color.rgb(148, 179, 199));
        paint.setColor(Color.rgb(232, 239, 242));
        c.drawRect(0, horizon, w, h, paint);

        // Distant snowy hills
        Path hills = new Path();
        hills.moveTo(0, horizon + 24);
        hills.lineTo(w * 0.12f, horizon - 26);
        hills.lineTo(w * 0.26f, horizon + 10);
        hills.lineTo(w * 0.43f, horizon - 44);
        hills.lineTo(w * 0.60f, horizon + 8);
        hills.lineTo(w * 0.76f, horizon - 30);
        hills.lineTo(w, horizon + 20);
        hills.close();
        paint.setColor(Color.rgb(215, 226, 231));
        c.drawPath(hills, paint);

        // Perspective road
        float center = w * 0.5f - roadOffset * 0.55f;
        float topHalf = w * 0.055f;
        float bottomHalf = w * 0.43f;
        Path road = new Path();
        road.moveTo(center - topHalf, horizon);
        road.lineTo(center + topHalf, horizon);
        road.lineTo(w * 0.5f + bottomHalf, h);
        road.lineTo(w * 0.5f - bottomHalf, h);
        road.close();
        paint.setColor(Color.rgb(76, 84, 89));
        c.drawPath(road, paint);

        // Snowy road shoulders
        paint.setStrokeWidth(Math.max(4f, w * 0.006f));
        paint.setColor(Color.WHITE);
        c.drawLine(center - topHalf, horizon, w * 0.5f - bottomHalf, h, paint);
        c.drawLine(center + topHalf, horizon, w * 0.5f + bottomHalf, h, paint);

        // Moving center dashes
        float phase = (distance * 2.5f) % 90f;
        for (int i = 0; i < 9; i++) {
            float z = (i * 90f + phase) / 810f;
            float p = z * z;
            float y = horizon + p * (h - horizon);
            float y2 = y + 10f + p * 30f;
            float x = center + (w * 0.5f - center) * p;
            paint.setStrokeWidth(2f + p * 10f);
            paint.setColor(Color.rgb(238, 209, 82));
            c.drawLine(x, y, x, y2, paint);
        }

        // Roadside buildings and trees
        drawRoadside(c, w, h, horizon, center);

        // Mission marker in the distance
        if (mission < 3) {
            float pulse = 1f + 0.12f * (float)Math.sin(distance * 0.08f);
            paint.setColor(Color.rgb(255, 210, 40));
            c.drawCircle(center, horizon + 58, 14f * pulse, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(Color.WHITE);
            c.drawCircle(center, horizon + 58, 19f * pulse, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Player pickup viewed from behind
        drawTruck(c, w, h);
    }

    private void drawRoadside(Canvas c, int w, int h, float horizon, float center) {
        float phase = (distance * 0.9f) % 120f;
        for (int i = 0; i < 8; i++) {
            float raw = (i * 120f + phase) / 960f;
            float p = raw * raw;
            float y = horizon + 15 + p * (h - horizon - 80);
            float scale = 0.22f + p * 1.1f;
            boolean leftSide = (i % 2 == 0);
            float roadEdge = leftSide
                    ? lerp(center - w * 0.055f, w * 0.5f - w * 0.43f, p)
                    : lerp(center + w * 0.055f, w * 0.5f + w * 0.43f, p);
            float x = roadEdge + (leftSide ? -1 : 1) * (60f + 85f * scale);

            if (i % 3 == 0) drawHouse(c, x, y, scale, leftSide);
            else drawTree(c, x, y, scale);
        }
    }

    private void drawHouse(Canvas c, float x, float y, float s, boolean flip) {
        float bw = 70f * s;
        float bh = 44f * s;
        paint.setColor(Color.rgb(119, 74, 53));
        c.drawRect(x - bw / 2, y - bh, x + bw / 2, y, paint);

        Path roof = new Path();
        roof.moveTo(x - bw * 0.62f, y - bh);
        roof.lineTo(x, y - bh - 30f * s);
        roof.lineTo(x + bw * 0.62f, y - bh);
        roof.close();
        paint.setColor(Color.rgb(245, 246, 242));
        c.drawPath(roof, paint);

        paint.setColor(Color.rgb(48, 60, 66));
        float doorW = 14f * s;
        c.drawRect(x + (flip ? -20f : 8f) * s, y - 27f * s,
                x + (flip ? -6f : 22f) * s, y, paint);
    }

    private void drawTree(Canvas c, float x, float y, float s) {
        paint.setColor(Color.rgb(91, 69, 52));
        c.drawRect(x - 4f * s, y - 30f * s, x + 4f * s, y, paint);
        paint.setColor(Color.rgb(38, 82, 65));
        for (int k = 0; k < 3; k++) {
            float yy = y - (26f + k * 18f) * s;
            Path p = new Path();
            p.moveTo(x, yy - 32f * s);
            p.lineTo(x - 26f * s, yy + 12f * s);
            p.lineTo(x + 26f * s, yy + 12f * s);
            p.close();
            c.drawPath(p, paint);
        }
        paint.setColor(Color.WHITE);
        c.drawCircle(x, y - 69f * s, 7f * s, paint);
    }

    private void drawTruck(Canvas c, int w, int h) {
        float cx = w * 0.5f + roadOffset * 0.15f;
        float baseY = h * 0.83f;
        float tw = Math.min(w * 0.19f, 260f);
        float th = tw * 0.48f;

        paint.setColor(Color.rgb(91, 48, 35));
        c.drawRoundRect(new RectF(cx - tw / 2, baseY - th, cx + tw / 2, baseY),
                14f, 14f, paint);
        paint.setColor(Color.rgb(126, 72, 50));
        c.drawRect(cx - tw * 0.28f, baseY - th * 1.42f,
                cx + tw * 0.28f, baseY - th * 0.66f, paint);
        paint.setColor(Color.rgb(125, 165, 177));
        c.drawRect(cx - tw * 0.20f, baseY - th * 1.31f,
                cx + tw * 0.20f, baseY - th * 0.82f, paint);
        paint.setColor(Color.rgb(27, 31, 33));
        c.drawCircle(cx - tw * 0.34f, baseY, th * 0.23f, paint);
        c.drawCircle(cx + tw * 0.34f, baseY, th * 0.23f, paint);
        paint.setColor(Color.rgb(220, 52, 42));
        c.drawRect(cx - tw * 0.42f, baseY - th * 0.45f, cx - tw * 0.32f, baseY - th * 0.25f, paint);
        c.drawRect(cx + tw * 0.32f, baseY - th * 0.45f, cx + tw * 0.42f, baseY - th * 0.25f, paint);
    }

    private void drawHud(Canvas c, int w, int h) {
        float pad = Math.max(12f, w * 0.012f);

        paint.setColor(Color.argb(175, 15, 23, 27));
        c.drawRoundRect(new RectF(pad, pad, w - pad, pad + h * 0.12f), 18f, 18f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.max(18f, h * 0.034f));
        c.drawText(message, pad * 1.7f, pad + h * 0.048f, paint);
        paint.setTextSize(Math.max(16f, h * 0.030f));
        c.drawText("速度  " + Math.round(speed * 2.2f) + " km/h", pad * 1.7f,
                pad + h * 0.095f, paint);

        float btn = Math.min(w, h) * 0.16f;
        float gap = btn * 0.12f;
        float bottom = h - pad;

        leftBtn.set(pad, bottom - btn, pad + btn, bottom);
        rightBtn.set(pad + btn + gap, bottom - btn, pad + btn * 2 + gap, bottom);
        brakeBtn.set(w - pad - btn * 2 - gap, bottom - btn, w - pad - btn - gap, bottom);
        accelBtn.set(w - pad - btn, bottom - btn, w - pad, bottom);

        drawButton(c, leftBtn, "◀", left);
        drawButton(c, rightBtn, "▶", right);
        drawButton(c, brakeBtn, "ブレーキ", brake);
        drawButton(c, accelBtn, "アクセル", accelerate);
    }

    private void drawButton(Canvas c, RectF r, String text, boolean pressed) {
        paint.setColor(pressed ? Color.argb(220, 235, 235, 235) : Color.argb(145, 20, 28, 32));
        c.drawRoundRect(r, 22f, 22f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);
        c.drawRoundRect(r, 22f, 22f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressed ? Color.rgb(28, 33, 35) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(18f, r.height() * 0.20f));
        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = r.centerY() - (fm.ascent + fm.descent) / 2f;
        c.drawText(text, r.centerX(), y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            left = right = accelerate = brake = false;
            return true;
        }

        boolean l = false, r = false, a = false, b = false;
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (leftBtn.contains(x, y)) l = true;
            if (rightBtn.contains(x, y)) r = true;
            if (accelBtn.contains(x, y)) a = true;
            if (brakeBtn.contains(x, y)) b = true;
        }
        left = l;
        right = r;
        accelerate = a;
        brake = b;
        return true;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
