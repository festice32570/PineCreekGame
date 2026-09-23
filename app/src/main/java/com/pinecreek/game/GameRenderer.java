package com.pinecreek.game;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {
    public interface Listener {
        void onHud(String status, String goal, String chapterName,
                   float enginePitch, float engineVolume);
        void onDialogue(String speaker, String line);
        void onSave(int campaign, int stage, float x, float z,
                    float heading, float speed, float fuel, int special);
        void onEnding(String title, String body);
    }

    public static final int CTRL_LEFT = 0;
    public static final int CTRL_RIGHT = 1;
    public static final int CTRL_BRAKE = 2;
    public static final int CTRL_ACCEL = 3;
    public static final int CTRL_REVERSE = 4;

    public static final int CAMPAIGN_WINTER = 0;
    public static final int CAMPAIGN_TRUCK = 1;
    public static final int CAMPAIGN_KEVIN = 2;
    public static final int CAMPAIGN_BLACKOUT = 3;
    public static final int CAMPAIGN_FREE = 99;

    private static final float ROAD_HALF = 6.5f;

    private final Listener listener;
    private final Random random = new Random(1337L);

    private int program;
    private int aPos;
    private int aNormal;
    private int uMvp;
    private int uModel;
    private int uColor;
    private int uLight;
    private int uFogColor;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] pv = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    private FloatBuffer cube;
    private FloatBuffer pyramid;
    private FloatBuffer cylinder;

    private volatile boolean left;
    private volatile boolean right;
    private volatile boolean brake;
    private volatile boolean accel;
    private volatile boolean reverse;
    private volatile boolean actionRequested;
    private volatile boolean honkRequested;
    private volatile boolean gameplay;

    private float px = 0f;
    private float pz = 22f;
    private float heading = 0f;
    private float signedSpeed = 0f;
    private float steeringAngle = 0f;
    private float fuel = 100f;
    private float worldTime = 0f;

    private int campaign = CAMPAIGN_WINTER;
    private int stage = 0;
    private int special = 0;
    private int towingMode = 0;
    private boolean endingShown = false;
    private float stormTimer = 90f;

    private long lastNs;
    private float hudTimer = 0f;
    private float radioTimer = 18f;
    private float eventTimer = 36f;
    private float saveTimer = 1f;
    private float actionCooldown = 0f;
    private float honkCooldown = 0f;
    private float bumpCooldown = 0f;
    private float safeX = 0f;
    private float safeZ = 22f;
    private float safeHeading = 0f;
    private float safeUpdateTimer = 0f;
    private float cameraHeading = 0f;
    private boolean cameraReady = false;
    private float viewAspect = 1.7778f;

    private float turkeyX = 8f;
    private float turkeyZ = -250f;
    private float turkeyPhase = 0f;

    private final ArrayList<House> houses = new ArrayList<>();
    private final ArrayList<Tree> trees = new ArrayList<>();
    private final ArrayList<Person> people = new ArrayList<>();
    private final ArrayList<Lamp> lamps = new ArrayList<>();

    private static class House {
        float x,z,w,h,d,r,g,b;
        int kind;
        House(float x,float z,float w,float h,float d,float r,float g,float b,int kind) {
            this.x=x; this.z=z; this.w=w; this.h=h; this.d=d;
            this.r=r; this.g=g; this.b=b; this.kind=kind;
        }
    }

    private static class Tree {
        float x,z,s;
        Tree(float x,float z,float s){this.x=x;this.z=z;this.s=s;}
    }

    private static class Person {
        float x,z,r,g,b;
        Person(float x,float z,float r,float g,float b){
            this.x=x;this.z=z;this.r=r;this.g=g;this.b=b;
        }
    }

    private static class Lamp {
        float x,z;
        Lamp(float x,float z){this.x=x;this.z=z;}
    }

    private static final String[][] RADIO = {
            {"町内放送", "本日の道路状況：白い。昨日も白かった。明日もたぶん白い。以上。"},
            {"町内放送", "公共交通のお知らせです。ありません。徒歩の場合は防寒具と強い意志をご用意ください。"},
            {"ラジオ", "気温マイナス19度。住民からは『今日は暖かい』との報告が入っています。"},
            {"町内放送", "路肩の青いピックアップは故障車ではありません。所有者によると『四月まで休憩中』です。"},
            {"ラジオ", "今夜の食堂はバーベキュー。小盛りをご注文の場合、厨房から健康状態を確認されます。"},
            {"町内放送", "除雪車を追い越さないでください。除雪車があなたを追い越そうとしている場合は、祈ってください。"},
            {"ラジオ", "中古車店からのお知らせ。『走れば車だ』。走らない場合は『可能性のある車』として販売中です。"},
            {"町内放送", "冬季に置き去りにされたトラックを勝手に町の共有車だと思わないでください。町長もそう言っています。たぶん。"}
    };

    private static final String[][] RANDOM_EVENTS = {
            {"住民無線", "誰かが冷蔵庫を玄関前に置いた。故障ではない。外気温の方が冷えるからだ。"},
            {"住民無線", "メイン通りにソファが落ちています。持ち主は『春までそこでもいい』と言っています。"},
            {"ラジオ", "ジムがまた『部品取り』を一台買いました。ナンバーが付いているので本人は完成車だと主張しています。"},
            {"町内放送", "雪だるまに反射ベストを着せた方へ。除雪担当が人間と間違えました。完成度は高かったそうです。"},
            {"住民無線", "誰かジャンパーケーブル持ってない？ ……いや、やっぱりボブがもう来た。"},
            {"ラジオ", "本日の生活情報。エンジンが一発で掛かった人は町役場へ報告しないでください。自慢になります。"},
            {"町内放送", "食堂前の巨大な肉の箱は落とし物ではありません。夕食です。"},
            {"住民無線", "道路脇のトラックを掘り出したら別のトラックが出てきた。所有者を確認中。"}
    };

    public GameRenderer(Listener listener) {
        this.listener = listener;
        buildTown();
    }

    public void setGameplay(boolean value) {
        gameplay = value;
        if (!value) {
            left=right=brake=accel=reverse=false;
            signedSpeed=0f;
            steeringAngle=0f;
        }
    }

    public void newGame(int newCampaign) {
        campaign = newCampaign;
        stage = 0;
        special = 0;
        towingMode = 0;
        endingShown = false;
        stormTimer = 90f;
        px = 0f;
        pz = 22f;
        heading = 0f;
        signedSpeed = 0f;
        steeringAngle = 0f;
        safeX = px;
        safeZ = pz;
        safeHeading = heading;
        cameraHeading = heading;
        cameraReady = true;
        fuel = 100f;
        gameplay = true;
        radioTimer = 22f;
        eventTimer = 34f;

        if (campaign == CAMPAIGN_FREE) {
            listener.onDialogue("町内放送",
                    "自由走行モード。目的はありません。常識もありません。好きな道を走ってください。");
        } else if (campaign == CAMPAIGN_WINTER) {
            listener.onDialogue("町内放送",
                    "初めての冬ですね。まず食堂へ巨大BBQの荷物を届けてください。道路が見えなくても道路はたぶんそこです。");
        } else if (campaign == CAMPAIGN_TRUCK) {
            listener.onDialogue("ジムの無線",
                    "大変だ。18台目のトラックが消えた。盗難じゃない。たぶんサイドブレーキを忘れて勝手に旅立った。うちへ来てくれ。");
        } else if (campaign == CAMPAIGN_KEVIN) {
            listener.onDialogue("町役場",
                    "緊急連絡。七面鳥ケビンが逃走しました。食堂のパンを持ったままです。まず役場へ来てください。");
        } else if (campaign == CAMPAIGN_BLACKOUT) {
            listener.onDialogue("町内放送",
                    "停電です。原因候補はジム宅のブロックヒーター17台です。町役場へ集合してください。");
        }
        saveNow();
    }

    public void loadGame(int savedCampaign, int savedStage, float x, float z,
                         float h, float speed, float savedFuel, int savedSpecial) {
        campaign = savedCampaign;
        stage = Math.max(0, savedStage);
        special = Math.max(0, savedSpecial);
        px = x;
        pz = z;
        heading = h;
        signedSpeed = Math.max(-VehiclePhysics.ROAD_REVERSE_SPEED,
                Math.min(VehiclePhysics.ROAD_TOP_SPEED, speed));
        steeringAngle = 0f;
        safeX = px;
        safeZ = pz;
        safeHeading = heading;
        cameraHeading = heading;
        cameraReady = true;
        fuel = Math.max(0f, Math.min(100f, savedFuel));
        gameplay = true;
        endingShown = false;
        stormTimer = 90f;
        inferTowState();
        listener.onDialogue("町内放送",
                "おかえりなさい。町はまだ雪の中です。特に改善していません。");
    }

    public void enterFreeRoam() {
        campaign = CAMPAIGN_FREE;
        stage = 0;
        special = 0;
        towingMode = 0;
        gameplay = true;
        endingShown = true;
        listener.onDialogue("ジム",
                "自由に走れ。トラックは引退しない。次の持ち主に移るだけだ。");
    }

    private void inferTowState() {
        towingMode = 0;
        if (campaign == CAMPAIGN_WINTER && stage == 2) towingMode = 1;
        if (campaign == CAMPAIGN_TRUCK && stage == 2) towingMode = 2;
        if (campaign == CAMPAIGN_BLACKOUT && stage >= 2 && stage <= 5) towingMode = 3;
    }

    public void control(int which, boolean value) {
        if (which == CTRL_LEFT) left = value;
        if (which == CTRL_RIGHT) right = value;
        if (which == CTRL_BRAKE) brake = value;
        if (which == CTRL_ACCEL) accel = value;
        if (which == CTRL_REVERSE) reverse = value;
    }

    public void action() {
        actionRequested = true;
    }

    public void honk() {
        honkRequested = true;
    }

    public void resetVehicle() {
        px = safeX;
        pz = safeZ;
        heading = safeHeading;
        signedSpeed = 0f;
        steeringAngle = 0f;
        cameraHeading = heading;
        cameraReady = true;
        listener.onDialogue("車内",
                "道路へ復帰。パイン・クリークでは『何もなかったことにする』のも重要な運転技術だ。");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        String vs =
                "uniform mat4 uMvp;" +
                "uniform mat4 uModel;" +
                "uniform vec3 uLight;" +
                "attribute vec3 aPos;" +
                "attribute vec3 aNormal;" +
                "varying float vLight;" +
                "varying float vDepth;" +
                "void main(){" +
                " vec3 n=normalize(mat3(uModel)*aNormal);" +
                " vLight=0.38+0.62*max(dot(n,normalize(uLight)),0.0);" +
                " gl_Position=uMvp*vec4(aPos,1.0);" +
                " vDepth=gl_Position.w;" +
                "}";

        String fs =
                "precision mediump float;" +
                "uniform vec3 uColor;" +
                "uniform vec3 uFogColor;" +
                "varying float vLight;" +
                "varying float vDepth;" +
                "void main(){" +
                " float fog=clamp((vDepth-85.0)/280.0,0.0,0.84);" +
                " vec3 lit=uColor*vLight;" +
                " gl_FragColor=vec4(mix(lit,uFogColor,fog),1.0);" +
                "}";

        program = link(vs, fs);
        aPos = GLES20.glGetAttribLocation(program,"aPos");
        aNormal = GLES20.glGetAttribLocation(program,"aNormal");
        uMvp = GLES20.glGetUniformLocation(program,"uMvp");
        uModel = GLES20.glGetUniformLocation(program,"uModel");
        uColor = GLES20.glGetUniformLocation(program,"uColor");
        uLight = GLES20.glGetUniformLocation(program,"uLight");
        uFogColor = GLES20.glGetUniformLocation(program,"uFogColor");

        cube = makeCube();
        pyramid = makePyramid();
        cylinder = makeCylinder(14);
        lastNs = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        viewAspect = (float)width / Math.max(1,height);
        Matrix.perspectiveM(projection,0,63f,viewAspect,.12f,560f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min(.04f,(now-lastNs)/1_000_000_000f);
        lastNs = now;
        worldTime += dt;

        if (gameplay) update(dt);
        else updateTitleWorld(dt);

        float daylight = .5f + .5f*(float)Math.sin(worldTime*.014f+.8f);
        float skyR = .30f+.31f*daylight;
        float skyG = .39f+.32f*daylight;
        float skyB = .50f+.31f*daylight;

        GLES20.glClearColor(skyR,skyG,skyB,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniform3f(uLight,-.38f,1f,.22f);
        GLES20.glUniform3f(uFogColor,skyR+.07f,skyG+.07f,skyB+.07f);

        if (gameplay) {
            float speedAbs = Math.abs(signedSpeed);
            float fov = 58f + Math.min(8f, speedAbs * .34f);
            Matrix.perspectiveM(projection,0,fov,viewAspect,.12f,560f);

            if (!cameraReady) {
                cameraHeading = heading;
                cameraReady = true;
            }

            // Chase-camera dead zone:
            // Steering first moves/rotates the truck inside the frame. The camera
            // does NOT immediately rotate with every steering input.
            float delta = VehiclePhysics.normalizeAngle(heading - cameraHeading);
            float deadZone = (float)Math.toRadians(10.0f);
            float absDelta = Math.abs(delta);
            boolean steeringNow = Math.abs(steeringAngle) > Math.toRadians(2.0f);

            if (steeringNow && absDelta > deadZone) {
                float excess = absDelta - deadZone;
                float follow = 1f - (float)Math.exp(-dt * (1.45f + speedAbs * .018f));
                cameraHeading = VehiclePhysics.normalizeAngle(
                        cameraHeading + Math.signum(delta) * excess * follow);
            } else if (!steeringNow) {
                // Once the driver straightens the wheel, gently recenter the camera.
                float follow = 1f - (float)Math.exp(-dt * (1.15f + speedAbs * .012f));
                cameraHeading = VehiclePhysics.normalizeAngle(cameraHeading + delta * follow);
            }

            float camBack = 12.6f + Math.min(4.4f,speedAbs*.18f);
            float camX = px - (float)Math.sin(cameraHeading)*camBack;
            float camZ = pz + (float)Math.cos(cameraHeading)*camBack;

            // Important: look direction follows CAMERA heading, not vehicle heading.
            // This is what lets the truck visibly travel left/right across the screen
            // before the camera catches up, like a normal third-person racer.
            float lookAhead = 6.2f + speedAbs*.07f;
            float lookX = px + (float)Math.sin(cameraHeading)*lookAhead;
            float lookZ = pz - (float)Math.cos(cameraHeading)*lookAhead;
            Matrix.setLookAtM(view,0,camX,6.15f,camZ,lookX,1.45f,lookZ,0,1,0);
        } else {
            Matrix.perspectiveM(projection,0,63f,viewAspect,.12f,560f);
            float a = worldTime*.075f;
            float cx = (float)Math.sin(a)*34f;
            float cz = -238f + (float)Math.cos(a)*34f;
            Matrix.setLookAtM(view,0,cx,15f,cz,0f,2.2f,-244f,0,1,0);
        }

        Matrix.multiplyMM(pv,0,projection,0,view,0);
        drawWorld(daylight);

        if (gameplay) {
            drawPlayerTruck();
            drawTowObject();
            drawMissionMarker();
        } else {
            parkedTruck(0,-231,.38f,.10f,.045f,0f);
        }

        drawTurkey();
        drawSnowflakes();
    }

    private void updateTitleWorld(float dt) {
        turkeyPhase += dt;
        updateTurkey();
    }

    private void update(float dt) {
        hudTimer -= dt;
        radioTimer -= dt;
        eventTimer -= dt;
        saveTimer -= dt;
        actionCooldown -= dt;
        honkCooldown -= dt;
        bumpCooldown -= dt;
        turkeyPhase += dt;
        updateTurkey();

        boolean road = onRoad(px,pz);
        float oldX = px;
        float oldZ = pz;
        float oldHeading = heading;

        VehiclePhysics.State physics = new VehiclePhysics.State(
                px, pz, heading, signedSpeed, steeringAngle);
        VehiclePhysics.Input input = new VehiclePhysics.Input();
        input.left = left;
        input.right = right;
        input.brake = brake;
        input.throttle = accel && fuel > 0f;
        input.reverse = reverse && fuel > 0f;

        // Split long render frames into short physics steps to keep turning
        // consistent at 30/60/120 Hz and during occasional frame drops.
        float physicsRemaining = dt;
        while (physicsRemaining > 0f) {
            float step = Math.min(physicsRemaining, 1f / 120f);
            VehiclePhysics.step(physics, input, road, towingMode != 0, step);
            physicsRemaining -= step;
        }

        px = physics.x;
        pz = physics.z;
        heading = physics.heading;
        signedSpeed = physics.speed;
        steeringAngle = physics.steer;

        if (fuel > 0f) {
            if (input.throttle) fuel = Math.max(0f, fuel - dt * .15f);
            else if (input.reverse) fuel = Math.max(0f, fuel - dt * .12f);
        }

        if (hitsBuilding(px,pz)) {
            px = oldX;
            pz = oldZ;
            heading = oldHeading;
            steeringAngle *= .35f;
            signedSpeed *= -.08f;
            if (bumpCooldown <= 0f) {
                bumpCooldown = 3f;
                listener.onDialogue("車内",
                        "ゴンッ。建物は雪と違って押しても動かない。少なくともこの町では。");
            }
        }

        safeUpdateTimer -= dt;
        if (road && !hitsBuilding(px,pz) && Math.abs(signedSpeed) < 18f && safeUpdateTimer <= 0f) {
            safeX = px;
            safeZ = pz;
            safeHeading = heading;
            safeUpdateTimer = 1.5f;
        }

        if (px < -59f || px > 59f || pz > 32f || pz < -625f) {
            px = Math.max(-58f,Math.min(58f,px));
            pz = Math.max(-620f,Math.min(31f,pz));
            signedSpeed *= -.12f;
            if (bumpCooldown <= 0f) {
                bumpCooldown = 4f;
                listener.onDialogue("町内放送",
                        "そこから先に道路はありません。地図にもありません。戻ってください。");
            }
        }

        if (honkRequested) {
            honkRequested = false;
            handleHonk();
        }
        if (actionRequested) {
            actionRequested = false;
            handleAction();
        }

        if (campaign == CAMPAIGN_WINTER && stage == 8) {
            stormTimer -= dt;
            if (stormTimer <= 0f) {
                stormTimer = 90f;
                listener.onDialogue("メアリーの無線",
                        "時間切れ！……でも肉は余ってる。もう一回来な。パイン・クリークでは失敗も大盛りだ。");
            }
        }

        if (radioTimer <= 0f) {
            radioTimer = 27f + random.nextFloat()*18f;
            String[] r = RADIO[random.nextInt(RADIO.length)];
            listener.onDialogue(r[0],r[1]);
        }

        if (eventTimer <= 0f) {
            eventTimer = 42f + random.nextFloat()*26f;
            String[] e = RANDOM_EVENTS[random.nextInt(RANDOM_EVENTS.length)];
            listener.onDialogue(e[0],e[1]);
        }

        if (fuel <= 0f && ((int)worldTime)%12 == 0 && actionCooldown <= 0f) {
            actionCooldown = 5f;
            listener.onDialogue("車内",
                    "エンジンが咳をして黙った。燃料計は今回は正しかったらしい。");
        }

        if (saveTimer <= 0f) {
            saveTimer = 4f;
            saveNow();
        }

        if (hudTimer <= 0f) {
            hudTimer = .16f;
            int kmh = Math.round(Math.abs(signedSpeed)*4.25f);
            String gear = signedSpeed < -.25f ? "R" : "D";
            String roadText = onRoad(px,pz) ? "圧雪路" : "深雪";
            String status = "速度 "+kmh+" km/h  "+gear+"\n燃料 "+Math.round(fuel)+"%　"+roadText+
                    "\n現在地 "+locationName();
            String goal = goalText();
            if (nearTarget() && campaign != CAMPAIGN_FREE && !needsHonk()) {
                goal += "　【アクション可能】";
            }
            listener.onHud(
                    status,
                    goal,
                    chapterText(),
                    VehiclePhysics.suggestedEnginePitch(signedSpeed),
                    VehiclePhysics.suggestedEngineVolume(signedSpeed));
        }
    }

    private void handleAction() {
        if (actionCooldown > 0f) return;
        actionCooldown = 1.1f;

        if (campaign == CAMPAIGN_FREE) {
            String[] free = {
                    "メアリー「自由時間？ じゃあ食堂に来な。普通盛りはまだ残ってる。普通かどうかは別として。」",
                    "ジム「暇なら19台目を探しに行くか？」",
                    "ボブ「何も壊れてない時こそ工具を買うんだ。」",
                    "町長「行政の相談？ 今は除雪ロープ持ってるから後にして。」"
            };
            listener.onDialogue("住民",free[random.nextInt(free.length)]);
            return;
        }

        if (needsHonk()) {
            listener.onDialogue("車内",
                    "今回はアクションボタンじゃない。警笛だ。ケビンは行政手続きを理解しない。");
            return;
        }

        if (!nearTarget()) {
            listener.onDialogue("車内",
                    "目的地はもう少し先だ。黄色い印の近くまで行こう。");
            return;
        }

        if (Math.abs(signedSpeed) > 1.3f) {
            listener.onDialogue("車内",
                    "まず止まろう。パイン・クリークでも会話しながら突っ込むのは失礼だ。");
            return;
        }

        if (campaign == CAMPAIGN_WINTER) actionWinter();
        else if (campaign == CAMPAIGN_TRUCK) actionTruck();
        else if (campaign == CAMPAIGN_KEVIN) actionKevin();
        else if (campaign == CAMPAIGN_BLACKOUT) actionBlackout();

        saveNow();
    }

    private void actionWinter() {
        switch(stage) {
            case 0:
                listener.onDialogue("メアリー（食堂）",
                        "BBQありがと。『小盛り』を頼む客が来たから厨房がざわついてる。次は町長だ。除雪車ごと埋まった。");
                stage = 1;
                break;
            case 1:
                listener.onDialogue("町長",
                        "除雪車が埋まった。だから私の私物ピックアップで除雪車を引く。行政手続き？ まず牽引ロープだ。");
                towingMode = 1;
                stage = 2;
                break;
            case 2:
                listener.onDialogue("町長",
                        "救出成功。市の除雪車を市長の私物で救った。会計処理は春に考える。ボブの給油所へ行ってくれ。");
                towingMode = 0;
                stage = 3;
                break;
            case 3:
                fuel = 100f;
                listener.onDialogue("ボブ（給油所）",
                        "満タンとジャンパーケーブルだ。寒さでバッテリーは死ぬ。住民はコーヒーを追加する。東の横道に一台いる。");
                stage = 4;
                break;
            case 4:
                listener.onDialogue("トラックの持ち主",
                        "助かった！ 故障じゃない、冬眠だ。四月まで寝かせる予定だった。次は中古車店を覗いていきな。");
                stage = 5;
                break;
            case 5:
                listener.onDialogue("ボブ（中古車店）",
                        "看板を読め。『走れば車だ』。走らない？ なら『将来性あり』だ。ジムがまた一台増やした。");
                stage = 6;
                break;
            case 6:
                listener.onDialogue("ジム",
                        "17台目だ。壊れてない。まだ交換してない部品が残ってるだけだ。ところで役場が七面鳥に占拠された。");
                stage = 7;
                special = 0;
                break;
            case 7:
                listener.onDialogue("町役場",
                        "ケビンはアクションでは動きません。警笛を3回お願いします。議会より話が早いです。");
                break;
            case 8:
                listener.onDialogue("メアリー（食堂）",
                        "間に合った！ 雪嵐の夜にBBQを届けられたらもう観光客じゃない。『初めての冬？ トラック買った？ 一回は埋まった？』……全部済んだね。");
                finishCampaign(
                        "PINE CREEK 町民認定",
                        "初めての冬を生き延びた。\n巨大BBQ、埋まった除雪車、バッテリー、17台目、そしてケビン。\n\nようこそ。春まで道路の場所は保証されません。");
                break;
        }
    }

    private void actionTruck() {
        switch(stage) {
            case 0:
                listener.onDialogue("ジム",
                        "18台目が消えた。昨夜ここに置いた。サイドブレーキ？ そういえば付いてたかな。東の坂を探してくれ。");
                stage = 1;
                break;
            case 1:
                listener.onDialogue("車内",
                        "雪山から赤茶色のピックアップが半分だけ出ている。ナンバーは合ってる。なぜかラジオだけ鳴っている。牽引する。");
                towingMode = 2;
                stage = 2;
                break;
            case 2:
                towingMode = 0;
                listener.onDialogue("ボブ（中古車店）",
                        "こいつ昨日まで俺の店にあった気もする。でもジムが『拾った』と言うならジムのだ。問題はバッテリーが無いことだ。給油所へ。");
                stage = 3;
                break;
            case 3:
                fuel = 100f;
                listener.onDialogue("ボブ（給油所）",
                        "中古のバッテリーだ。『まだ電気が入ってる』という理由で良品扱い。ジムのところへ持っていけ。");
                stage = 4;
                break;
            case 4:
                listener.onDialogue("ジム",
                        "掛かった！ 18台目完成！ ……ん？ あの坂からもう一台転がってきてる。19台目かな。今日は忙しい。");
                finishCampaign(
                        "18台目、そして19台目",
                        "消えたトラックは戻った。\nその直後、次のトラックが自力でやってきた。\n\nジムの庭に空きスペースは無い。だが本人には見えている。");
                break;
        }
    }

    private void actionKevin() {
        switch(stage) {
            case 0:
                listener.onDialogue("町役場",
                        "ケビンが逃げた。食堂のパン袋も消えた。職員証は持っていないので安心してください。まず食堂へ。");
                stage = 1;
                break;
            case 1:
                listener.onDialogue("メアリー（食堂）",
                        "パンは盗られた。犯人は羽毛付き。給油所の方向へ走ったよ。車より速かった。");
                stage = 2;
                special = 0;
                break;
            case 2:
                listener.onDialogue("ボブ",
                        "あいつポンプの横から動かん。警笛を3回やれ。俺は燃料より七面鳥を怖がる日が来るとは思わなかった。");
                break;
            case 3:
                listener.onDialogue("ボブ（中古車店）",
                        "ケビンがこのトラックの荷台に入った。『走れば車だ』とは言ったが『七面鳥が乗れば公共交通』とは言ってない。役場へ戻してくれ。");
                stage = 4;
                break;
            case 4:
                listener.onDialogue("町役場",
                        "ケビン帰還。パン袋も回収。中身はありません。本人は反省していません。");
                finishCampaign(
                        "ケビンの大脱走",
                        "町役場、食堂、給油所、中古車店を巻き込んだ逃走劇は終了。\n\nケビンは翌朝また玄関前にいた。\nこの町では、それを平常運転と呼ぶ。");
                break;
        }
    }

    private void actionBlackout() {
        switch(stage) {
            case 0:
                listener.onDialogue("町長",
                        "停電だ。原因はたぶんジムのブロックヒーター17台。公共事業ヤードの発電機を持ってきてくれ。");
                stage = 1;
                break;
            case 1:
                listener.onDialogue("公共事業ヤード",
                        "発電機をトレーラーごと接続した。重い。古い。音が大きい。つまり町の設備として完璧だ。");
                towingMode = 3;
                stage = 2;
                break;
            case 2:
                listener.onDialogue("メアリー（食堂）",
                        "電気が戻った！ 冷蔵庫より外の方が寒いけど照明は必要だからね。ついでにBBQをボブへ届けて。");
                stage = 3;
                break;
            case 3:
                fuel = 100f;
                listener.onDialogue("ボブ（給油所）",
                        "BBQありがとう。東の道で一台バッテリーが上がってる。停電中にヘッドライト点けっぱなし。芸術点は高い。");
                stage = 4;
                break;
            case 4:
                listener.onDialogue("立ち往生した住民",
                        "掛かった！ 発電機を引きながら救援に来る人は初めて見た。役場へ戻して町全体を点けてくれ。");
                stage = 5;
                break;
            case 5:
                towingMode = 0;
                listener.onDialogue("町長",
                        "復旧！ ……あ、また消えた。ジムが18台目のブロックヒーターを挿したらしい。まあ原因が分かっただけ前進だ。");
                finishCampaign(
                        "停電の夜",
                        "発電機は町を救った。\nそして18本目の延長コードが町を再び暗くした。\n\nPine Creekでは『原因が分かる停電』は成功扱いらしい。");
                break;
        }
    }

    private void handleHonk() {
        if (honkCooldown > 0f) return;
        honkCooldown = 1.1f;

        if (needsHonk() && dist(px,pz,turkeyX,turkeyZ) < 19f) {
            special++;

            if (special == 1) {
                listener.onDialogue("ケビン（七面鳥）",
                        "ゴボゴボゴボッ！！　警笛に抗議しつつ二歩だけ譲った。");
            } else if (special == 2) {
                listener.onDialogue("町長",
                        "効いてる！ もう一回だ！ この町で最も迅速な行政手続きだ！");
            } else {
                if (campaign == CAMPAIGN_WINTER) {
                    listener.onDialogue("町役場",
                            "ケビン退去確認。明日また来る可能性は高いです。食堂へ急いでください。雪嵐が来ます。");
                    stage = 8;
                    special = 0;
                    stormTimer = 90f;
                } else if (campaign == CAMPAIGN_KEVIN) {
                    listener.onDialogue("ボブ",
                            "逃げた！ 今度は中古車店だ。あいつトラックの荷台を巣だと思ってる。");
                    stage = 3;
                    special = 0;
                }
                saveNow();
            }
            return;
        }

        String[] lines = {
                "一回なら挨拶。二回なら助けてくれ。三回ならブレーキが無い。",
                "警笛が元気ならまだ走れる。たぶん。",
                "誰かが手を振った。手袋が厚すぎて親指かどうかは分からない。",
                "遠くで別のトラックも警笛を返した。会話が成立したらしい。"
        };
        listener.onDialogue("近くの住民",lines[random.nextInt(lines.length)]);
    }

    private boolean needsHonk() {
        return (campaign == CAMPAIGN_WINTER && stage == 7)
                || (campaign == CAMPAIGN_KEVIN && stage == 2);
    }

    private void finishCampaign(String title, String body) {
        stage++;
        towingMode = 0;
        signedSpeed = 0f;
        saveNow();
        if (!endingShown) {
            endingShown = true;
            listener.onEnding(title,body);
        }
    }

    private boolean nearTarget() {
        if (campaign == CAMPAIGN_FREE) return false;
        return dist(px,pz,targetX(),targetZ()) < 13.5f;
    }

    private float targetX() {
        if (campaign == CAMPAIGN_WINTER) {
            switch(stage) {
                case 0: return -15f;
                case 1: return 16f;
                case 2: return -42f;
                case 3: return -16f;
                case 4: return 34f;
                case 5: return -18f;
                case 6: return 19f;
                case 7: return turkeyX;
                case 8: return -15f;
            }
        } else if (campaign == CAMPAIGN_TRUCK) {
            switch(stage) {
                case 0: return 19f;
                case 1: return 44f;
                case 2: return -18f;
                case 3: return -16f;
                case 4: return 19f;
            }
        } else if (campaign == CAMPAIGN_KEVIN) {
            switch(stage) {
                case 0: return 16f;
                case 1: return -15f;
                case 2: return turkeyX;
                case 3: return -18f;
                case 4: return 16f;
            }
        } else if (campaign == CAMPAIGN_BLACKOUT) {
            switch(stage) {
                case 0: return 16f;
                case 1: return -42f;
                case 2: return -15f;
                case 3: return -16f;
                case 4: return 34f;
                case 5: return 16f;
            }
        }
        return 0f;
    }

    private float targetZ() {
        if (campaign == CAMPAIGN_WINTER) {
            switch(stage) {
                case 0: return -108f;
                case 1: return -244f;
                case 2: return -244f;
                case 3: return -358f;
                case 4: return -358f;
                case 5: return -486f;
                case 6: return -486f;
                case 7: return turkeyZ;
                case 8: return -108f;
            }
        } else if (campaign == CAMPAIGN_TRUCK) {
            switch(stage) {
                case 0: return -486f;
                case 1: return -358f;
                case 2: return -486f;
                case 3: return -358f;
                case 4: return -486f;
            }
        } else if (campaign == CAMPAIGN_KEVIN) {
            switch(stage) {
                case 0: return -244f;
                case 1: return -108f;
                case 2: return turkeyZ;
                case 3: return -486f;
                case 4: return -244f;
            }
        } else if (campaign == CAMPAIGN_BLACKOUT) {
            switch(stage) {
                case 0: return -244f;
                case 1: return -244f;
                case 2: return -108f;
                case 3: return -358f;
                case 4: return -358f;
                case 5: return -244f;
            }
        }
        return 0f;
    }

    private String chapterText() {
        if (campaign == CAMPAIGN_FREE) return "自由走行";

        if (campaign == CAMPAIGN_WINTER) {
            String[] c = {
                    "序章　初めての冬",
                    "第1章　町長と除雪車",
                    "第1章　除雪車救出",
                    "第2章　バッテリーの町",
                    "第2章　四月まで休憩中",
                    "第3章　走れば車だ",
                    "第4章　17台目",
                    "第5章　役場のケビン",
                    "最終章　春まで"
            };
            return c[Math.min(stage,c.length-1)];
        }

        if (campaign == CAMPAIGN_TRUCK) {
            return "外伝　18台目のトラック";
        }

        if (campaign == CAMPAIGN_KEVIN) {
            return "外伝　ケビンの大脱走";
        }

        return "外伝　停電の夜";
    }

    private String goalText() {
        if (campaign == CAMPAIGN_FREE) {
            return "目的なし。町を走って妙な日常を探せ";
        }

        if (campaign == CAMPAIGN_WINTER) {
            switch(stage) {
                case 0: return "巨大BBQの荷物を食堂へ届けろ";
                case 1: return "町役場で埋まった除雪車を確認";
                case 2: return "除雪車を公共事業ヤードまで牽引";
                case 3: return "給油所で燃料とジャンパーケーブルを受け取る";
                case 4: return "東の横道で冬眠中のトラックを救援";
                case 5: return "ボブ中古車店『走れば車だ』へ";
                case 6: return "ジムの17台目のトラックを確認";
                case 7: return "役場の七面鳥ケビンに警笛を3回　"+special+"/3";
                case 8: return "雪嵐のBBQ緊急便　残り "+Math.max(0,(int)stormTimer)+"秒";
            }
        } else if (campaign == CAMPAIGN_TRUCK) {
            switch(stage) {
                case 0: return "ジムの庭で18台目の失踪を聞く";
                case 1: return "東の坂で逃走したトラックを探す";
                case 2: return "18台目をボブ中古車店まで牽引";
                case 3: return "給油所で『まだ電気がある』バッテリーを入手";
                case 4: return "ジムへ18台目を返す";
            }
        } else if (campaign == CAMPAIGN_KEVIN) {
            switch(stage) {
                case 0: return "町役場でケビン逃走事件を聞く";
                case 1: return "食堂で手掛かりを探す";
                case 2: return "給油所付近のケビンに警笛を3回　"+special+"/3";
                case 3: return "中古車店でケビンを回収";
                case 4: return "ケビンを町役場へ戻す";
            }
        } else if (campaign == CAMPAIGN_BLACKOUT) {
            switch(stage) {
                case 0: return "停電した町役場へ";
                case 1: return "公共事業ヤードで発電機を牽引";
                case 2: return "発電機を食堂へ運ぶ";
                case 3: return "BBQを給油所へ届ける";
                case 4: return "東の横道でバッテリー上がりを救援";
                case 5: return "発電機を町役場へ運ぶ";
            }
        }
        return "自由走行";
    }

    private String locationName() {
        if (dist(px,pz,-15,-108)<28) return "家族食堂";
        if (dist(px,pz,16,-244)<30) return "町役場";
        if (dist(px,pz,-42,-244)<26) return "公共事業ヤード";
        if (dist(px,pz,-16,-358)<30) return "給油所";
        if (dist(px,pz,-18,-486)<28) return "ボブ中古車店";
        if (dist(px,pz,19,-486)<28) return "ジムの庭";
        if (!onRoad(px,pz)) return "雪原";
        return "メイン通り";
    }

    private void saveNow() {
        listener.onSave(campaign,stage,px,pz,heading,signedSpeed,fuel,special);
    }

    private boolean onRoad(float x,float z) {
        if (Math.abs(x) <= ROAD_HALF) return true;
        for (float cross : new float[]{-108f,-244f,-358f,-486f}) {
            if (Math.abs(z-cross) <= ROAD_HALF && Math.abs(x) < 60f) return true;
        }
        return false;
    }

    private boolean hitsBuilding(float x,float z) {
        for (House h : houses) {
            if (Math.abs(x-h.x) < h.w*.5f+1.35f
                    && Math.abs(z-h.z) < h.d*.5f+1.95f) {
                return true;
            }
        }
        return false;
    }

    private void updateTurkey() {
        float baseX = 8f;
        float baseZ = -250f;

        if (campaign == CAMPAIGN_KEVIN && stage == 2) {
            baseX = -8f;
            baseZ = -352f;
        }

        turkeyX = baseX + (float)Math.sin(turkeyPhase*1.55f)*6.5f;
        turkeyZ = baseZ + (float)Math.cos(turkeyPhase*1.12f)*5f;
    }

    private void buildTown() {
        houses.add(new House(-15,-108,11,5.4f,9,.52f,.10f,.065f,1));
        houses.add(new House( 16,-244,13,7.5f,10,.65f,.65f,.61f,2));
        houses.add(new House(-16,-358,12,5.3f,9,.09f,.29f,.42f,3));
        houses.add(new House(-18,-486,12,5.1f,9,.50f,.18f,.07f,4));
        houses.add(new House( 19,-486,13,4.8f,10,.34f,.23f,.14f,5));
        houses.add(new House(-42,-244,12,4.2f,10,.23f,.30f,.33f,6));

        for (int i=0;i<26;i++) {
            float z = 10-i*24f+random.nextFloat()*8f;
            float side = random.nextBoolean() ? -1f : 1f;
            float x = side*(15f+random.nextFloat()*16f);
            houses.add(new House(
                    x,z,
                    6.5f+random.nextFloat()*4f,
                    3.8f+random.nextFloat()*2f,
                    5.5f+random.nextFloat()*3f,
                    .24f+random.nextFloat()*.30f,
                    .12f+random.nextFloat()*.18f,
                    .08f+random.nextFloat()*.14f,
                    0));
        }

        for (float z : new float[]{-108f,-244f,-358f,-486f}) {
            for (int j=-2;j<=2;j++) {
                if (j==0) continue;
                float x=j*24f;
                if (Math.abs(z+244f)<1 && j==-2) continue;
                houses.add(new House(
                        x,
                        z+(j%2==0?16f:-16f),
                        7f+random.nextFloat()*3f,
                        4f+random.nextFloat()*2f,
                        7f,
                        .25f+random.nextFloat()*.25f,
                        .12f,
                        .09f,
                        0));
            }
        }

        for (int i=0;i<125;i++) {
            float z=25-random.nextFloat()*665f;
            float side=random.nextBoolean()?-1f:1f;
            float x=side*(10f+random.nextFloat()*48f);
            trees.add(new Tree(x,z,.62f+random.nextFloat()*.95f));
        }

        people.add(new Person(-9,-104,.80f,.25f,.18f));
        people.add(new Person(10,-240,.20f,.34f,.70f));
        people.add(new Person(-10,-354,.73f,.48f,.16f));
        people.add(new Person(12,-482,.20f,.48f,.24f));
        people.add(new Person(-13,-482,.68f,.18f,.15f));
        people.add(new Person(32,-354,.22f,.40f,.66f));

        for (float z=-46;z>-610;z-=40f) {
            lamps.add(new Lamp(-9f,z));
            lamps.add(new Lamp(9f,z));
        }
    }

    private void drawWorld(float daylight) {
        box(0,-.55f,-300f,124f,1f,690f,.88f,.92f,.94f,0);
        box(0,.02f,-300f,13f,.12f,668f,.15f,.18f,.20f,0);

        for (float z : new float[]{-108f,-244f,-358f,-486f}) {
            box(0,.025f,z,118f,.13f,13f,.16f,.18f,.20f,0);
        }

        for (float z=20;z>-625;z-=13f) {
            box(0,.11f,z,.16f,.04f,4.3f,.94f,.72f,.11f,0);
        }

        for (float cz : new float[]{-108f,-244f,-358f,-486f}) {
            for (float x=-54;x<=54;x+=13f) {
                box(x,.11f,cz,4.3f,.04f,.16f,.94f,.72f,.11f,0);
            }
        }

        for (float z=20;z>-625;z-=17f) {
            box(-7.25f,.35f,z,1.1f,.7f,10f,.94f,.96f,.97f,0);
            box( 7.25f,.35f,z,1.1f,.7f,10f,.94f,.96f,.97f,0);
        }

        for (House h : houses) {
            if (!gameplay || dist(px,pz,h.x,h.z)<300f) drawHouse(h);
        }

        for (Tree t : trees) {
            if (!gameplay || dist(px,pz,t.x,t.z)<260f) drawTree(t);
        }

        for (Person p : people) {
            if (!gameplay || dist(px,pz,p.x,p.z)<180f) drawPerson(p);
        }

        boolean lampsOn = daylight < .36f;
        for (Lamp l : lamps) {
            if (gameplay && dist(px,pz,l.x,l.z)>190f) continue;
            box(l.x,2f,l.z,.18f,4f,.18f,.07f,.07f,.07f,0);
            float c=lampsOn?.96f:.45f;
            box(l.x,4.12f,l.z,.58f,.38f,.58f,c,c*.87f,.34f,0);
        }

        parkedTruck(-22,-140,.08f,.24f,.38f,.12f);
        parkedTruck( 24,-292,.42f,.10f,.055f,-.65f);
        parkedTruck( 34,-358,.30f,.32f,.17f,.55f);
        parkedTruck(-27,-486,.38f,.08f,.055f,.35f);
        parkedTruck(-35,-486,.08f,.27f,.35f,-.20f);
        parkedTruck( 28,-486,.16f,.31f,.18f,.10f);
        parkedTruck( 38,-486,.46f,.12f,.05f,-.25f);
        parkedTruck( 47,-486,.28f,.22f,.13f,.20f);

        drawSign(-18,-475,.74f,.16f,.05f);
        drawSign(-42,-234,.24f,.31f,.34f);

        if (!(campaign==CAMPAIGN_WINTER && stage==2)) {
            drawSnowPlow(12,-252,0f);
        }

        for (int i=0;i<14;i++) {
            float z=-22-i*44f;
            float side=(i%2==0?-1:1);
            box(side*(8.2f+(i%3)*1.2f),.9f,z,2.6f,1.8f,9f,.94f,.96f,.97f,.08f*side);
        }
    }

    private void drawHouse(House h) {
        box(h.x,h.h*.5f,h.z,h.w,h.h,h.d,h.r,h.g,h.b,0);
        pyramid(h.x,h.h+1.35f,h.z,h.w*1.16f,2.8f,h.d*1.08f,.82f,.84f,.84f,0);

        float front=h.z+h.d*.505f;
        box(h.x-h.w*.23f,h.h*.58f,front,h.w*.20f,h.h*.25f,.08f,.50f,.69f,.76f,0);
        box(h.x+h.w*.23f,h.h*.58f,front,h.w*.20f,h.h*.25f,.08f,.50f,.69f,.76f,0);

        if (h.kind==1) {
            box(h.x,2.0f,front+.38f,h.w*.80f,.32f,1.25f,.76f,.10f,.065f,0);
        } else if (h.kind==2) {
            box(h.x-3.4f,2.3f,front+.8f,.6f,4.6f,.6f,.80f,.80f,.76f,0);
            box(h.x+3.4f,2.3f,front+.8f,.6f,4.6f,.6f,.80f,.80f,.76f,0);
        } else if (h.kind==3) {
            box(h.x,4.9f,h.z+6.2f,13f,.5f,7f,.87f,.87f,.84f,0);
            box(h.x-5f,2.4f,h.z+6.2f,.45f,4.8f,.45f,.72f,.72f,.70f,0);
            box(h.x+5f,2.4f,h.z+6.2f,.45f,4.8f,.45f,.72f,.72f,.70f,0);
            box(h.x-2.2f,.9f,h.z+6.2f,1.2f,1.8f,.8f,.70f,.08f,.06f,0);
            box(h.x+2.2f,.9f,h.z+6.2f,1.2f,1.8f,.8f,.70f,.08f,.06f,0);
        } else if (h.kind==4) {
            box(h.x,3.0f,front+.45f,h.w*.92f,.35f,1.4f,.88f,.72f,.12f,0);
        } else if (h.kind==5) {
            box(h.x,h.h*.48f,front+.05f,h.w*.55f,h.h*.62f,.08f,.20f,.20f,.18f,0);
        } else if (h.kind==6) {
            box(h.x,h.h*.48f,front+.05f,h.w*.62f,h.h*.70f,.08f,.13f,.16f,.18f,0);
        }
    }

    private void drawTree(Tree t) {
        float s=t.s;
        box(t.x,1.3f*s,t.z,.32f*s,2.6f*s,.32f*s,.18f,.11f,.06f,0);
        pyramid(t.x,3.1f*s,t.z,3.6f*s,3.8f*s,3.6f*s,.055f,.23f,.13f,0);
        pyramid(t.x,4.6f*s,t.z,2.8f*s,3.2f*s,2.8f*s,.06f,.28f,.15f,0);
        pyramid(t.x,5.65f*s,t.z,1.1f*s,1.1f*s,1.1f*s,.94f,.96f,.97f,0);
    }

    private void drawPerson(Person p) {
        box(p.x,1.1f,p.z,.64f,1.55f,.46f,p.r,p.g,p.b,0);
        box(p.x,2.05f,p.z,.62f,.62f,.62f,.82f,.62f,.45f,0);
        box(p.x-.20f,.25f,p.z,.17f,.7f,.20f,.10f,.10f,.10f,0);
        box(p.x+.20f,.25f,p.z,.17f,.7f,.20f,.10f,.10f,.10f,0);
    }

    private void drawSign(float x,float z,float r,float g,float b) {
        box(x,1.7f,z,.20f,3.4f,.20f,.15f,.12f,.08f,0);
        box(x,3.3f,z,5.4f,1.8f,.35f,r,g,b,0);
        box(x,3.3f,z-.20f,4.4f,.15f,.10f,.93f,.88f,.62f,0);
    }

    private void parkedTruck(float x,float z,float r,float g,float b,float rot) {
        if (gameplay && dist(px,pz,x,z)>220f) return;
        drawDetailedTruck(x,z,rot,r,g,b,0f,false);
    }

    private void drawPlayerTruck() {
        drawDetailedTruck(px,pz,heading,.38f,.10f,.045f,steeringAngle,true);

        if (campaign==CAMPAIGN_WINTER && stage==0) {
            boxLocal(px,pz,heading,-.70f,1.66f,1.58f,1.0f,.72f,1.0f,.46f,.25f,.08f);
            boxLocal(px,pz,heading,.58f,1.62f,1.38f,1.0f,.70f,1.0f,.56f,.31f,.10f);
        }
    }

    private void drawDetailedTruck(float x,float z,float rot,
                                   float r,float g,float b,
                                   float frontSteer, boolean snow) {
        // chassis and lower body
        boxLocal(x,z,rot,0,.78f,.10f,3.45f,.40f,5.35f,.09f,.09f,.085f);
        boxLocal(x,z,rot,0,1.08f,.15f,3.30f,.95f,5.10f,r,g,b);

        // hood and front nose
        boxLocal(x,z,rot,0,1.62f,-1.55f,3.08f,.54f,2.05f,r*.94f,g*.94f,b*.94f);
        boxLocal(x,z,rot,0,1.30f,-2.52f,3.18f,.78f,.28f,.18f,.18f,.17f);

        // cab
        boxLocal(x,z,rot,0,2.02f,.05f,2.78f,1.55f,2.35f,r*.92f,g*.92f,b*.92f);
        boxLocal(x,z,rot,0,2.16f,-1.12f,2.34f,.84f,.07f,.16f,.42f,.50f);
        boxLocal(x,z,rot,-1.405f,2.02f,.03f,.08f,.82f,1.58f,.14f,.37f,.45f);
        boxLocal(x,z,rot, 1.405f,2.02f,.03f,.08f,.82f,1.58f,.14f,.37f,.45f);

        // bed
        boxLocal(x,z,rot,0,1.24f,1.72f,3.22f,.55f,2.25f,r*.84f,g*.84f,b*.84f);
        boxLocal(x,z,rot,-1.48f,1.58f,1.72f,.22f,.80f,2.30f,r,g,b);
        boxLocal(x,z,rot, 1.48f,1.58f,1.72f,.22f,.80f,2.30f,r,g,b);
        boxLocal(x,z,rot,0,1.58f,2.78f,3.05f,.80f,.18f,r*.95f,g*.95f,b*.95f);

        // grille slats, headlights, bumper, plate
        for (int i=-2;i<=2;i++) {
            boxLocal(x,z,rot,i*.48f,1.38f,-2.69f,.34f,.10f,.08f,.055f,.055f,.052f);
        }
        boxLocal(x,z,rot,-1.05f,1.56f,-2.70f,.58f,.42f,.09f,.94f,.80f,.38f);
        boxLocal(x,z,rot, 1.05f,1.56f,-2.70f,.58f,.42f,.09f,.94f,.80f,.38f);
        boxLocal(x,z,rot,0,.98f,-2.75f,3.35f,.25f,.16f,.55f,.57f,.58f);
        boxLocal(x,z,rot,0,1.02f,-2.85f,.75f,.26f,.06f,.78f,.78f,.74f);

        // rear bumper and tail lights
        boxLocal(x,z,rot,0,.93f,2.82f,3.28f,.22f,.16f,.56f,.57f,.58f);
        boxLocal(x,z,rot,-1.22f,1.38f,2.86f,.38f,.38f,.08f,.78f,.06f,.04f);
        boxLocal(x,z,rot, 1.22f,1.38f,2.86f,.38f,.38f,.08f,.78f,.06f,.04f);

        // mirrors
        boxLocal(x,z,rot,-1.72f,2.28f,-.55f,.34f,.22f,.48f,.055f,.055f,.055f);
        boxLocal(x,z,rot, 1.72f,2.28f,-.55f,.34f,.22f,.48f,.055f,.055f,.055f);

        // wheels - front wheels visually steer
        drawWheelLocal(x,z,rot,-1.63f,.62f,-1.72f,frontSteer);
        drawWheelLocal(x,z,rot, 1.63f,.62f,-1.72f,frontSteer);
        drawWheelLocal(x,z,rot,-1.63f,.62f, 1.72f,0f);
        drawWheelLocal(x,z,rot, 1.63f,.62f, 1.72f,0f);

        // wheel arches / fenders
        boxLocal(x,z,rot,-1.47f,1.00f,-1.72f,.32f,.38f,1.15f,r*.72f,g*.72f,b*.72f);
        boxLocal(x,z,rot, 1.47f,1.00f,-1.72f,.32f,.38f,1.15f,r*.72f,g*.72f,b*.72f);
        boxLocal(x,z,rot,-1.47f,1.00f, 1.72f,.32f,.38f,1.15f,r*.72f,g*.72f,b*.72f);
        boxLocal(x,z,rot, 1.47f,1.00f, 1.72f,.32f,.38f,1.15f,r*.72f,g*.72f,b*.72f);

        if (snow) {
            boxLocal(x,z,rot,0,1.93f,-1.52f,2.70f,.08f,1.55f,.92f,.95f,.96f);
            boxLocal(x,z,rot,0,2.83f,.05f,2.30f,.07f,1.62f,.92f,.95f,.96f);
        }
    }

    private void drawWheelLocal(float ox,float oz,float truckRot,
                                float lx,float y,float lz,float steer) {
        float sn=(float)Math.sin(truckRot);
        float cs=(float)Math.cos(truckRot);
        float wx=ox+lx*cs+lz*sn;
        float wz=oz-lx*sn+lz*cs;
        drawMesh(
                cylinder,
                wx,y,wz,
                .72f,.72f,.48f,
                .025f,.025f,.025f,
                0f,
                truckRot+steer,
                (float)Math.toRadians(90f));
        // silver hub
        drawMesh(
                cylinder,
                wx,y,wz,
                .34f,.34f,.50f,
                .48f,.48f,.46f,
                0f,
                truckRot+steer,
                (float)Math.toRadians(90f));
    }

    private void drawSnowPlow(float x,float z,float rot) {
        box(x,1.15f,z,4.0f,1.6f,6.0f,.72f,.46f,.08f,rot);
        boxLocal(x,z,rot,0,2.15f,.55f,3.1f,1.7f,2.7f,.78f,.53f,.10f);
        boxLocal(x,z,rot,0,.75f,-3.4f,6.0f,1.2f,.45f,.82f,.82f,.78f);
        for (float wx : new float[]{-1.9f,1.9f}) {
            for (float wz : new float[]{-1.9f,1.9f}) {
                drawWheelLocal(x,z,rot,wx,.55f,wz,0f);
            }
        }
    }

    private void drawGeneratorTrailer(float x,float z,float rot) {
        box(x,.60f,z,3.0f,.30f,4.5f,.16f,.16f,.15f,rot);
        boxLocal(x,z,rot,0,1.35f,.25f,2.45f,1.8f,2.6f,.72f,.55f,.08f);
        boxLocal(x,z,rot,0,1.45f,-1.12f,1.8f,.85f,.12f,.15f,.15f,.14f);
        boxLocal(x,z,rot,0,.62f,-2.75f,.16f,.16f,2.1f,.20f,.18f,.14f);
        drawWheelLocal(x,z,rot,-1.48f,.52f,.65f,0f);
        drawWheelLocal(x,z,rot, 1.48f,.52f,.65f,0f);
    }

    private void drawTowObject() {
        if (towingMode == 0) return;

        float back = 8.2f;
        float tx = px-(float)Math.sin(heading)*back;
        float tz = pz+(float)Math.cos(heading)*back;

        if (towingMode == 1) {
            drawSnowPlow(tx,tz,heading);
        } else if (towingMode == 2) {
            drawDetailedTruck(tx,tz,heading,.34f,.13f,.07f,0f,false);
        } else if (towingMode == 3) {
            drawGeneratorTrailer(tx,tz,heading);
        }

        for (int i=1;i<=5;i++) {
            float t=i/6f;
            float rx=px+(tx-px)*t;
            float rz=pz+(tz-pz)*t;
            box(rx,.54f,rz,.08f,.08f,1.05f,.16f,.11f,.07f,heading);
        }
    }

    private void drawMissionMarker() {
        if (campaign == CAMPAIGN_FREE) return;

        float tx=targetX();
        float tz=targetZ();
        if (dist(px,pz,tx,tz)>320f) return;

        float bob=4.2f+(float)Math.sin(worldTime*2.5f)*.65f;
        pyramid(tx,bob,tz,2.5f,3.6f,2.5f,.96f,.68f,.04f,worldTime*.8f);
        box(tx,.07f,tz,4.8f,.09f,4.8f,.86f,.54f,.04f,worldTime*.45f);
    }

    private void drawTurkey() {
        if (gameplay && dist(px,pz,turkeyX,turkeyZ)>190f) return;

        box(turkeyX,.78f,turkeyZ,.92f,1.15f,.78f,.38f,.21f,.09f,turkeyPhase);
        box(turkeyX,1.48f,turkeyZ-.18f,.42f,.42f,.42f,.59f,.10f,.065f,turkeyPhase);

        for (int i=-2;i<=2;i++) {
            float a=turkeyPhase+i*.38f;
            float fx=turkeyX+(float)Math.sin(a)*.68f;
            float fz=turkeyZ+(float)Math.cos(a)*.68f;
            box(fx,.98f,fz,.23f,1.12f,.12f,.46f,.10f+.07f*(i+2),.06f,a);
        }
    }

    private void drawSnowflakes() {
        float ox=gameplay?px:0f;
        float oz=gameplay?pz:-244f;

        for (int i=0;i<48;i++) {
            float a=i*1.71f+worldTime*.17f;
            float radius=8f+(i%10)*3.4f;
            float sx=ox+(float)Math.sin(a)*radius;
            float sz=oz+(float)Math.cos(a*.83f)*radius;
            float sy=.8f+positiveMod(i*1.83f-worldTime*2.8f,8f);
            box(sx,sy,sz,.065f,.065f,.065f,.97f,.98f,1f,0);
        }
    }

    private void boxLocal(float ox,float oz,float rot,float lx,float y,float lz,
                          float sx,float sy,float sz,float r,float g,float b) {
        float sn=(float)Math.sin(rot);
        float cs=(float)Math.cos(rot);
        float wx=ox+lx*cs+lz*sn;
        float wz=oz-lx*sn+lz*cs;
        box(wx,y,wz,sx,sy,sz,r,g,b,rot);
    }

    private void box(float x,float y,float z,float sx,float sy,float sz,
                     float r,float g,float b,float rotY) {
        drawMesh(cube,x,y,z,sx,sy,sz,r,g,b,0f,rotY,0f);
    }

    private void pyramid(float x,float y,float z,float sx,float sy,float sz,
                         float r,float g,float b,float rotY) {
        drawMesh(pyramid,x,y,z,sx,sy,sz,r,g,b,0f,rotY,0f);
    }

    private void drawMesh(FloatBuffer mesh,float x,float y,float z,
                          float sx,float sy,float sz,float r,float g,float b,
                          float rotX,float rotY,float rotZ) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.rotateM(model,0,(float)Math.toDegrees(rotY),0,1,0);
        Matrix.rotateM(model,0,(float)Math.toDegrees(rotX),1,0,0);
        Matrix.rotateM(model,0,(float)Math.toDegrees(rotZ),0,0,1);
        Matrix.scaleM(model,0,sx*.5f,sy*.5f,sz*.5f);
        Matrix.multiplyMM(mvp,0,pv,0,model,0);

        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
        GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
        GLES20.glUniform3f(uColor,r,g,b);

        mesh.position(0);
        GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,24,mesh);
        GLES20.glEnableVertexAttribArray(aPos);

        mesh.position(3);
        GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,24,mesh);
        GLES20.glEnableVertexAttribArray(aNormal);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,mesh.capacity()/6);
    }

    private FloatBuffer makeCube() {
        ArrayList<Float> v=new ArrayList<>();
        face(v,0,0,-1,new float[][]{{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1}});
        face(v,0,0,1,new float[][]{{1,-1,1},{-1,-1,1},{-1,1,1},{1,1,1}});
        face(v,-1,0,0,new float[][]{{-1,-1,1},{-1,-1,-1},{-1,1,-1},{-1,1,1}});
        face(v,1,0,0,new float[][]{{1,-1,-1},{1,-1,1},{1,1,1},{1,1,-1}});
        face(v,0,1,0,new float[][]{{-1,1,-1},{1,1,-1},{1,1,1},{-1,1,1}});
        face(v,0,-1,0,new float[][]{{-1,-1,1},{1,-1,1},{1,-1,-1},{-1,-1,-1}});
        return toBuffer(v);
    }

    private void face(ArrayList<Float> out,float nx,float ny,float nz,float[][] p) {
        int[] idx={0,1,2,0,2,3};
        for (int i : idx) {
            out.add(p[i][0]); out.add(p[i][1]); out.add(p[i][2]);
            out.add(nx); out.add(ny); out.add(nz);
        }
    }

    private FloatBuffer makePyramid() {
        ArrayList<Float> v=new ArrayList<>();
        tri(v,new float[]{-1,-1,-1},new float[]{1,-1,-1},new float[]{0,1,0});
        tri(v,new float[]{1,-1,-1},new float[]{1,-1,1},new float[]{0,1,0});
        tri(v,new float[]{1,-1,1},new float[]{-1,-1,1},new float[]{0,1,0});
        tri(v,new float[]{-1,-1,1},new float[]{-1,-1,-1},new float[]{0,1,0});
        face(v,0,-1,0,new float[][]{{-1,-1,1},{1,-1,1},{1,-1,-1},{-1,-1,-1}});
        return toBuffer(v);
    }

    private FloatBuffer makeCylinder(int segments) {
        ArrayList<Float> v=new ArrayList<>();

        for (int i=0;i<segments;i++) {
            double a0=2*Math.PI*i/segments;
            double a1=2*Math.PI*(i+1)/segments;
            float x0=(float)Math.cos(a0);
            float z0=(float)Math.sin(a0);
            float x1=(float)Math.cos(a1);
            float z1=(float)Math.sin(a1);

            addVertex(v,x0,-1,z0,x0,0,z0);
            addVertex(v,x1,-1,z1,x1,0,z1);
            addVertex(v,x1, 1,z1,x1,0,z1);

            addVertex(v,x0,-1,z0,x0,0,z0);
            addVertex(v,x1, 1,z1,x1,0,z1);
            addVertex(v,x0, 1,z0,x0,0,z0);

            addVertex(v,0,1,0,0,1,0);
            addVertex(v,x1,1,z1,0,1,0);
            addVertex(v,x0,1,z0,0,1,0);

            addVertex(v,0,-1,0,0,-1,0);
            addVertex(v,x0,-1,z0,0,-1,0);
            addVertex(v,x1,-1,z1,0,-1,0);
        }

        return toBuffer(v);
    }

    private void addVertex(ArrayList<Float> out,
                           float x,float y,float z,
                           float nx,float ny,float nz) {
        out.add(x); out.add(y); out.add(z);
        out.add(nx); out.add(ny); out.add(nz);
    }

    private void tri(ArrayList<Float> out,float[] a,float[] b,float[] c) {
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy;
        float ny=uz*vx-ux*vz;
        float nz=ux*vy-uy*vx;
        float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        nx/=len; ny/=len; nz/=len;

        for (float[] p : new float[][]{a,b,c}) {
            out.add(p[0]); out.add(p[1]); out.add(p[2]);
            out.add(nx); out.add(ny); out.add(nz);
        }
    }

    private FloatBuffer toBuffer(ArrayList<Float> src) {
        FloatBuffer b=ByteBuffer.allocateDirect(src.size()*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (float f : src) b.put(f);
        b.position(0);
        return b;
    }

    private int shader(int type,String code) {
        int s=GLES20.glCreateShader(type);
        GLES20.glShaderSource(s,code);
        GLES20.glCompileShader(s);
        return s;
    }

    private int link(String vs,String fs) {
        int p=GLES20.glCreateProgram();
        GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,vs));
        GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fs));
        GLES20.glLinkProgram(p);
        return p;
    }

    private static float moveToward(float value,float target,float amount) {
        if (value<target) return Math.min(target,value+amount);
        if (value>target) return Math.max(target,value-amount);
        return value;
    }

    private static float positiveMod(float v,float m) {
        float r=v%m;
        return r<0?r+m:r;
    }

    private static float dist(float ax,float az,float bx,float bz) {
        float dx=ax-bx;
        float dz=az-bz;
        return (float)Math.sqrt(dx*dx+dz*dz);
    }
}
