package com.pinecreek.game;

import android.annotation.SuppressLint;
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
import android.widget.ImageView;
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
    private FrameLayout storyOverlay;
    private FrameLayout endingOverlay;
    private Button continueButton;

    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        immersive();

        prefs = getSharedPreferences("pine_creek_save_v2", MODE_PRIVATE);
        audio = new AudioEngine();

        root = new FrameLayout(this);
        game = new GameSurface(this, this);
        root.addView(game, new FrameLayout.LayoutParams(-1, -1));

        buildHud();
        buildControls();
        buildTitle();
        buildStorySelector();
        buildEnding();

        setContentView(root);
        audio.start();
        audio.setMenuMode(true);
        game.renderer.setGameplay(false);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void buildHud() {
        hud = panelText(16, 0xC7152026);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        hp.setMargins(14, 14, 0, 0);
        root.addView(hud, hp);

        objective = panelText(16, 0xD91B242A);
        objective.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams op = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .59f),
                -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        op.topMargin = 14;
        root.addView(objective, op);

        chapter = panelText(14, 0xB51C252B);
        chapter.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, 14, 14, 0);
        root.addView(chapter, cp);

        dialogue = panelText(18, 0xEE151A1D);
        dialogue.setVisibility(View.GONE);
        dialogue.setMaxLines(3);
        dialogue.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .64f),
                -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        // Keep dialogue below the objective bar and away from the truck/controls.
        // The left HUD ends before this centered panel begins on phone layouts.
        dp.topMargin = dp(76);
        root.addView(dialogue, dp);

        setGameUi(false);
    }

    private void buildControls() {
        leftPad = new LinearLayout(this);
        leftPad.setOrientation(LinearLayout.HORIZONTAL);

        Button left = gameButton("◀");
        Button right = gameButton("▶");
        Button horn = gameButton("警笛");
        leftPad.addView(left);
        leftPad.addView(right);
        leftPad.addView(horn);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.BOTTOM);
        lp.setMargins(12, 0, 0, 12);
        root.addView(leftPad, lp);

        rightPad = new LinearLayout(this);
        rightPad.setOrientation(LinearLayout.HORIZONTAL);

        Button action = gameButton("アクション");
        Button reset = gameButton("復帰");
        Button reverse = gameButton("バック");
        Button brake = gameButton("ブレーキ");
        Button accel = gameButton("アクセル");

        rightPad.addView(action);
        rightPad.addView(reset);
        rightPad.addView(reverse);
        rightPad.addView(brake);
        rightPad.addView(accel);

        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.BOTTOM);
        rp.setMargins(0, 0, 12, 12);
        root.addView(rightPad, rp);

        bindHold(left, GameRenderer.CTRL_LEFT);
        bindHold(right, GameRenderer.CTRL_RIGHT);
        bindHold(reverse, GameRenderer.CTRL_REVERSE);
        bindHold(brake, GameRenderer.CTRL_BRAKE);
        bindHold(accel, GameRenderer.CTRL_ACCEL);

        horn.setOnClickListener(v -> {
            audio.horn();
            game.renderer.honk();
        });
        action.setOnClickListener(v -> game.renderer.action());
        reset.setOnClickListener(v -> game.queueEvent(() -> game.renderer.resetVehicle()));

        leftPad.setVisibility(View.GONE);
        rightPad.setVisibility(View.GONE);
    }

    private void buildTitle() {
        titleOverlay = new FrameLayout(this);
        titleOverlay.setBackgroundColor(Color.BLACK);

        // Fill the landscape side areas with the same supplied artwork, then
        // draw a full uncropped copy over it. This avoids plain black bars while
        // preserving the logo, truck and turkey exactly as provided.
        ImageView backdrop = new ImageView(this);
        backdrop.setImageResource(R.drawable.pine_creek_keyart);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setAlpha(.34f);
        titleOverlay.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.pine_creek_keyart);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        titleOverlay.addView(art, new FrameLayout.LayoutParams(-1, -1));

        // Slight dark veil only at the bottom so buttons stay readable while
        // leaving the supplied artwork itself clearly visible.
        View bottomShade = new View(this);
        bottomShade.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams shadeLp = new FrameLayout.LayoutParams(
                -1, dp(82), Gravity.BOTTOM);
        titleOverlay.addView(bottomShade, shadeLp);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        menu.setGravity(Gravity.CENTER);
        menu.setPadding(dp(10), dp(8), dp(10), dp(8));

        Button newGame = gameButton("ストーリー");
        continueButton = gameButton("続きから");
        Button free = gameButton("自由走行");
        Button how = gameButton("遊び方");

        Button[] buttons = { newGame, continueButton, free, how };
        for (Button b : buttons) {
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    0, dp(58), 1f);
            bp.leftMargin = dp(5);
            bp.rightMargin = dp(5);
            b.setLayoutParams(bp);
            menu.addView(b);
        }

        newGame.setOnClickListener(v -> {
            titleOverlay.setVisibility(View.GONE);
            storyOverlay.setVisibility(View.VISIBLE);
        });
        continueButton.setOnClickListener(v -> continueGame());
        free.setOnClickListener(v -> startCampaign(GameRenderer.CAMPAIGN_FREE));
        how.setOnClickListener(v -> Toast.makeText(
                this,
                "左・右：ハンドル\nアクセル：前進　バック：後退\nブレーキ：減速・停止\n復帰：最後に安全だった道路へ戻る\n黄色い印でアクション\n旋回時は車が先に画面内を動き、カメラは遅れて追従します。\n雪原は滑りやすく、速度も落ちます。",
                Toast.LENGTH_LONG).show());

        continueButton.setEnabled(prefs.getBoolean("has_save", false));

        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                -1, dp(74), Gravity.BOTTOM);
        menuLp.leftMargin = dp(12);
        menuLp.rightMargin = dp(12);
        menuLp.bottomMargin = dp(4);
        titleOverlay.addView(menu, menuLp);

        root.addView(titleOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void buildStorySelector() {
        storyOverlay = new FrameLayout(this);
        storyOverlay.setBackgroundColor(0xB20A1116);
        storyOverlay.setVisibility(View.GONE);

        LinearLayout card = menuCard();

        TextView t = new TextView(this);
        t.setText("ストーリー選択");
        t.setTextColor(Color.WHITE);
        t.setTextSize(27);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, 12);
        card.addView(t);

        Button winter = menuButton("初めての冬\nBBQ・除雪車・町民認定");
        Button truck = menuButton("18台目のトラック\n消えた廃車を追え");
        Button kevin = menuButton("ケビンの大脱走\n七面鳥 vs 町全体");
        Button blackout = menuButton("停電の夜\n発電機と17台のブロックヒーター");
        Button back = menuButton("戻る");

        card.addView(winter);
        card.addView(truck);
        card.addView(kevin);
        card.addView(blackout);
        card.addView(back);

        winter.setOnClickListener(v -> startCampaign(GameRenderer.CAMPAIGN_WINTER));
        truck.setOnClickListener(v -> startCampaign(GameRenderer.CAMPAIGN_TRUCK));
        kevin.setOnClickListener(v -> startCampaign(GameRenderer.CAMPAIGN_KEVIN));
        blackout.setOnClickListener(v -> startCampaign(GameRenderer.CAMPAIGN_BLACKOUT));
        back.setOnClickListener(v -> {
            storyOverlay.setVisibility(View.GONE);
            titleOverlay.setVisibility(View.VISIBLE);
        });

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .58f),
                -2, Gravity.CENTER);
        storyOverlay.addView(card, cp);
        root.addView(storyOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void buildEnding() {
        endingOverlay = new FrameLayout(this);
        endingOverlay.setBackgroundColor(0xC511171B);
        endingOverlay.setVisibility(View.GONE);

        LinearLayout card = menuCard();

        TextView endingTitle = new TextView(this);
        endingTitle.setId(View.generateViewId());
        endingTitle.setTag("ending_title");
        endingTitle.setTextColor(Color.WHITE);
        endingTitle.setTextSize(29);
        endingTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        endingTitle.setGravity(Gravity.CENTER);
        card.addView(endingTitle);

        TextView endingBody = new TextView(this);
        endingBody.setId(View.generateViewId());
        endingBody.setTag("ending_body");
        endingBody.setTextColor(0xFFE4E8E9);
        endingBody.setTextSize(17);
        endingBody.setGravity(Gravity.CENTER);
        endingBody.setPadding(0, 16, 0, 18);
        card.addView(endingBody);

        Button free = menuButton("この町を自由に走る");
        Button another = menuButton("別のストーリー");
        Button title = menuButton("タイトルへ戻る");

        card.addView(free);
        card.addView(another);
        card.addView(title);

        free.setOnClickListener(v -> {
            endingOverlay.setVisibility(View.GONE);
            setGameUi(true);
            audio.setMenuMode(false);
            game.renderer.enterFreeRoam();
        });
        another.setOnClickListener(v -> {
            endingOverlay.setVisibility(View.GONE);
            setGameUi(false);
            game.renderer.setGameplay(false);
            storyOverlay.setVisibility(View.VISIBLE);
            audio.setMenuMode(true);
        });
        title.setOnClickListener(v -> showTitle());

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * .57f),
                -2, Gravity.CENTER);
        endingOverlay.addView(card, cp);
        root.addView(endingOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private LinearLayout menuCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(30, 22, 30, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE8172127);
        bg.setCornerRadius(28f);
        bg.setStroke(2, 0x66FFFFFF);
        card.setBackground(bg);
        return card;
    }

    private void startCampaign(int campaign) {
        titleOverlay.setVisibility(View.GONE);
        storyOverlay.setVisibility(View.GONE);
        endingOverlay.setVisibility(View.GONE);
        setGameUi(true);
        audio.setMenuMode(false);
        prefs.edit().clear().apply();

        if (campaign == GameRenderer.CAMPAIGN_FREE) {
            game.renderer.newGame(GameRenderer.CAMPAIGN_FREE);
        } else {
            game.renderer.newGame(campaign);
        }
    }

    private void continueGame() {
        titleOverlay.setVisibility(View.GONE);
        storyOverlay.setVisibility(View.GONE);
        endingOverlay.setVisibility(View.GONE);
        setGameUi(true);
        audio.setMenuMode(false);

        int campaign = prefs.getInt("campaign", GameRenderer.CAMPAIGN_WINTER);
        int stage = prefs.getInt("stage", 0);
        float x = prefs.getFloat("x", 0f);
        float z = prefs.getFloat("z", 22f);
        float heading = prefs.getFloat("heading", 0f);
        float speed = prefs.getFloat("speed", 0f);
        float fuel = prefs.getFloat("fuel", 100f);
        int special = prefs.getInt("special", 0);

        game.renderer.loadGame(campaign, stage, x, z, heading, speed, fuel, special);
    }

    private void showTitle() {
        setGameUi(false);
        storyOverlay.setVisibility(View.GONE);
        endingOverlay.setVisibility(View.GONE);
        titleOverlay.setVisibility(View.VISIBLE);
        continueButton.setEnabled(prefs.getBoolean("has_save", false));
        game.renderer.setGameplay(false);
        audio.setMenuMode(true);
    }

    private void setGameUi(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        if (hud != null) hud.setVisibility(v);
        if (objective != null) objective.setVisibility(v);
        if (chapter != null) chapter.setVisibility(v);
        if (leftPad != null) leftPad.setVisibility(v);
        if (rightPad != null) rightPad.setVisibility(v);
        if (!show && dialogue != null) dialogue.setVisibility(View.GONE);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private TextView panelText(int sp, int color) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(14, 9, 14, 9);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(15f);
        bg.setStroke(2, 0x44FFFFFF);
        t.setBackground(bg);
        return t;
    }

    private Button gameButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(92);
        b.setMinHeight(78);
        b.setPadding(8, 4, 8, 4);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xC020292F);
        bg.setCornerRadius(20f);
        bg.setStroke(2, 0xCCFFFFFF);
        b.setBackground(bg);
        return b;
    }

    private Button menuButton(String label) {
        Button b = gameButton(label);
        b.setMinHeight(62);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = 8;
        b.setLayoutParams(p);
        return b;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindHold(Button b, int control) {
        b.setOnTouchListener((v, e) -> {
            int action = e.getActionMasked();
            boolean down = action != MotionEvent.ACTION_UP
                    && action != MotionEvent.ACTION_CANCEL;
            game.renderer.control(control, down);
            if (action == MotionEvent.ACTION_UP) v.performClick();
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
            dialogue.setText(getString(R.string.dialogue_format, speaker, line));
            dialogue.setVisibility(View.VISIBLE);
            audio.event();

            ui.removeCallbacksAndMessages(null);
            ui.postDelayed(() -> dialogue.setVisibility(View.GONE), 5600);
        });
    }

    @Override
    public void onSave(int campaign, int stage, float x, float z,
                       float heading, float speed, float fuel, int special) {
        if (campaign == GameRenderer.CAMPAIGN_FREE) return;

        prefs.edit()
                .putBoolean("has_save", true)
                .putInt("campaign", campaign)
                .putInt("stage", stage)
                .putFloat("x", x)
                .putFloat("z", z)
                .putFloat("heading", heading)
                .putFloat("speed", speed)
                .putFloat("fuel", fuel)
                .putInt("special", special)
                .apply();
    }

    @Override
    public void onEnding(String title, String body) {
        runOnUiThread(() -> {
            setGameUi(false);
            TextView t = endingOverlay.findViewWithTag("ending_title");
            TextView b = endingOverlay.findViewWithTag("ending_body");
            t.setText(title);
            b.setText(body);
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

[executed on device: festice-virtual-machine (07fc5208-706b-4ca8-850a-ef91db884468)]