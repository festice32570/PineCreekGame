package com.pinecreek.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements GameRenderer.Listener {
    private FrameLayout root;
    private GameSurface game;
    private AudioEngine audio;
    private SharedPreferences prefs;
    private TextView hud;
    private TextView objective;
    private TextView dialogue;
    private TextView chapter;
    private LinearLayout leftPad;
    private LinearLayout rightPad;
    private FrameLayout titleOverlay;
    private FrameLayout endingOverlay;
    private Button continueButton;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        immersive();

        prefs = getSharedPreferences("pine_creek_save", MODE_PRIVATE);
        audio = new AudioEngine();

        root = new FrameLayout(this);
        game = new GameSurface(this, this);
        root.addView(game, new FrameLayout.LayoutParams(-1, -1));

        buildHud();
        buildControls();
        buildTitle();
        buildEnding();

        setContentView(root);
        audio.start();
        game.renderer.setGameplay(false);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void buildHud() {
        hud = panelText(17, 0xC7152026);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        hp.setMargins(16, 16, 0, 0);
        root.addView(hud, hp);

        objective = panelText(17, 0xD91B242A);
        objective.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams op = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .63f), -2,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        op.topMargin = 16;
        root.addView(objective, op);

        chapter = panelText(15, 0xA91C252B);
        chapter.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        cp.setMargins(0, 16, 16, 0);
        root.addView(chapter, cp);

        dialogue = panelText(20, 0xEE151A1D);
        dialogue.setVisibility(View.GONE);
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .72f), -2,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dp.bottomMargin = 22;
        root.addView(dialogue, dp);

        hud.setVisibility(View.GONE);
        objective.setVisibility(View.GONE);
        chapter.setVisibility(View.GONE);
    }

    private void buildControls() {
        leftPad = new LinearLayout(this);
        leftPad.setOrientation(LinearLayout.HORIZONTAL);
        Button left = gameButton("左");
        Button right = gameButton("右");
        Button horn = gameButton("警笛");
        leftPad.addView(left);
        leftPad.addView(right);
        leftPad.addView(horn);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.LEFT | Gravity.BOTTOM);
        lp.setMargins(14, 0, 0, 14);
        root.addView(leftPad, lp);

        rightPad = new LinearLayout(this);
        rightPad.setOrientation(LinearLayout.HORIZONTAL);
        Button action = gameButton("アクション");
        Button brake = gameButton("ブレーキ");
        Button accel = gameButton("アクセル");
        rightPad.addView(action);
        rightPad.addView(brake);
        rightPad.addView(accel);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.BOTTOM);
        rp.setMargins(0, 0, 14, 14);
        root.addView(rightPad, rp);

        bindHold(left, 0);
        bindHold(right, 1);
        bindHold(brake, 2);
        bindHold(accel, 3);
        horn.setOnClickListener(v -> {
            audio.horn();
            game.renderer.honk();
        });
        action.setOnClickListener(v -> game.renderer.action());

        leftPad.setVisibility(View.GONE);
        rightPad.setVisibility(View.GONE);
    }

    private void buildTitle() {
        titleOverlay = new FrameLayout(this);
        titleOverlay.setBackgroundColor(0x8D0B1318);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(34, 28, 34, 30);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE8172127);
        bg.setCornerRadius(28f);
        bg.setStroke(2, 0x66FFFFFF);
        card.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("PINE CREEK\n冬の町");
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(36);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText("人口は少ない。トラックは多い。常識は春まで雪の下。");
        sub.setTextColor(0xFFE0E5E7);
        sub.setTextSize(16);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 8, 0, 22);
        card.addView(sub);

        Button newGame = menuButton("ニューゲーム");
        continueButton = menuButton("続きから");
        Button how = menuButton("遊び方");
        card.addView(newGame);
        card.addView(continueButton);
        card.addView(how);

        newGame.setOnClickListener(v -> startGame(true));
        continueButton.setOnClickListener(v -> startGame(false));
        how.setOnClickListener(v -> Toast.makeText(this,
                "左・右で操舵。アクセル／ブレーキで運転。黄色い印の近くでアクション。\n雪原は滑りやすく遅い。警笛は住民にも七面鳥にも効く。",
                Toast.LENGTH_LONG).show());

        continueButton.setEnabled(prefs.getBoolean("has_save", false));

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .55f), -2,
                Gravity.CENTER);
        titleOverlay.addView(card, cardLp);
        root.addView(titleOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void buildEnding() {
        endingOverlay = new FrameLayout(this);
        endingOverlay.setBackgroundColor(0xC511171B);
        endingOverlay.setVisibility(View.GONE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(38, 30, 38, 32);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF01A2429);
        bg.setCornerRadius(28f);
        bg.setStroke(2, 0x77FFFFFF);
        card.setBackground(bg);

        TextView t = new TextView(this);
        t.setText("PINE CREEK 町民認定");
        t.setTextColor(Color.WHITE);
        t.setTextSize(31);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        card.addView(t);

        TextView body = new TextView(this);
        body.setText("「初めての冬？」\n「トラックは買った？」\n「一回は埋まった？」\n\n全部済んだ。ようこそ。\nなお春になるまで道路の場所は保証されません。");
        body.setTextColor(0xFFE4E8E9);
        body.setTextSize(18);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, 18, 0, 20);
        card.addView(body);

        Button free = menuButton("自由走行を続ける");
        Button title = menuButton("タイトルへ戻る");
        card.addView(free);
        card.addView(title);

        free.setOnClickListener(v -> {
            endingOverlay.setVisibility(View.GONE);
            game.renderer.enterFreeRoam();
            setGameUi(true);
        });
        title.setOnClickListener(v -> showTitle());

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .56f), -2,
                Gravity.CENTER);
        endingOverlay.addView(card, cp);
        root.addView(endingOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void startGame(boolean fresh) {
        titleOverlay.setVisibility(View.GONE);
        endingOverlay.setVisibility(View.GONE);
        setGameUi(true);
        audio.setMenuMode(false);

        if (fresh) {
            prefs.edit().clear().apply();
            game.renderer.newGame();
        } else {
            int stage = prefs.getInt("stage", 0);
            float x = prefs.getFloat("x", 0f);
            float z = prefs.getFloat("z", 22f);
            float heading = prefs.getFloat("heading", 0f);
            float fuel = prefs.getFloat("fuel", 100f);
            int turkeyHits = prefs.getInt("turkey_hits", 0);
            game.renderer.loadGame(stage, x, z, heading, fuel, turkeyHits);
        }
    }

    private void showTitle() {
        setGameUi(false);
        endingOverlay.setVisibility(View.GONE);
        titleOverlay.setVisibility(View.VISIBLE);
        continueButton.setEnabled(prefs.getBoolean("has_save", false));
        game.renderer.setGameplay(false);
        audio.setMenuMode(true);
    }

    private void setGameUi(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        hud.setVisibility(v);
        objective.setVisibility(v);
        chapter.setVisibility(v);
        leftPad.setVisibility(v);
        rightPad.setVisibility(v);
        if (!show) dialogue.setVisibility(View.GONE);
    }

    private TextView panelText(int sp, int color) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(16, 10, 16, 10);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(16f);
        bg.setStroke(2, 0x44FFFFFF);
        t.setBackground(bg);
        return t;
    }

    private Button gameButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinWidth(104);
        b.setMinHeight(82);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xC020292F);
        bg.setCornerRadius(20f);
        bg.setStroke(2, 0xCCFFFFFF);
        b.setBackground(bg);
        return b;
    }

    private Button menuButton(String label) {
        Button b = gameButton(label);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = 10;
        b.setLayoutParams(p);
        return b;
    }

    private void bindHold(Button b, int control) {
        b.setOnTouchListener((v, e) -> {
            boolean down = e.getActionMasked() != MotionEvent.ACTION_UP
                    && e.getActionMasked() != MotionEvent.ACTION_CANCEL;
            game.renderer.control(control, down);
            return true;
        });
    }

    @Override
    public void onHud(String status, String goal, String chapterName,
                      float enginePitch, float engineVolume) {
        runOnUiThread(() -> {
            hud.setText(status);
            objective.setText(goal);
            chapter.setText(chapterName);
            audio.setEngine(enginePitch, engineVolume);
        });
    }

    @Override
    public void onDialogue(String speaker, String line) {
        runOnUiThread(() -> {
            dialogue.setText(speaker + "\n" + line);
            dialogue.setVisibility(View.VISIBLE);
            audio.event();
            ui.removeCallbacksAndMessages(null);
            ui.postDelayed(() -> dialogue.setVisibility(View.GONE), 5500);
        });
    }

    @Override
    public void onSave(int stage, float x, float z, float heading, float fuel, int turkeyHits) {
        prefs.edit()
                .putBoolean("has_save", true)
                .putInt("stage", stage)
                .putFloat("x", x)
                .putFloat("z", z)
                .putFloat("heading", heading)
                .putFloat("fuel", fuel)
                .putInt("turkey_hits", turkeyHits)
                .apply();
    }

    @Override
    public void onEnding() {
        runOnUiThread(() -> {
            setGameUi(false);
            endingOverlay.setVisibility(View.VISIBLE);
            audio.setMenuMode(true);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (game != null) game.onPause();
        if (audio != null) audio.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        immersive();
        if (game != null) game.onResume();
        if (audio != null) audio.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audio != null) audio.release();
    }
}
