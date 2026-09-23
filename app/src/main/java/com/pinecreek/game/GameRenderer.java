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
        void onSave(int stage, float x, float z, float heading, float fuel, int turkeyHits);
        void onEnding();
    }

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

    private volatile boolean left;
    private volatile boolean right;
    private volatile boolean brake;
    private volatile boolean accel;
    private volatile boolean actionRequested;
    private volatile boolean honkRequested;
    private volatile boolean gameplay;

    private float px = 0f;
    private float pz = 22f;
    private float heading = 0f;
    private float vx = 0f;
    private float vz = 0f;
    private float fuel = 100f;
    private float worldTime = 0f;

    private int stage = 0;
    private int turkeyHits = 0;
    private boolean towingPlow = false;
    private boolean endingShown = false;
    private float stormTimer = 90f;

    private long lastNs;
    private float hudTimer = 0f;
    private float radioTimer = 20f;
    private float saveTimer = 1f;
    private float actionCooldown = 0f;
    private float honkCooldown = 0f;
    private float bumpCooldown = 0f;

    private float turkeyX = 3f;
    private float turkeyZ = -252f;
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

    public GameRenderer(Listener listener) {
        this.listener = listener;
        buildTown();
    }

    public void setGameplay(boolean value) {
        gameplay = value;
        if (!value) {
            left=right=brake=accel=false;
            vx=vz=0f;
        }
    }

    public void newGame() {
        stage=0;
        turkeyHits=0;
        towingPlow=false;
        endingShown=false;
        stormTimer=90f;
        px=0f;
        pz=22f;
        heading=0f;
        vx=vz=0f;
        fuel=100f;
        gameplay=true;
        radioTimer=24f;
        listener.onDialogue("町内放送",
                "パイン・クリークへようこそ。初めての冬ですね。食堂へBBQの荷物を届けてください。なお道路が見えなくても道路はたぶんそこです。");
        saveNow();
    }

    public void loadGame(int savedStage, float x, float z, float h, float savedFuel, int savedTurkeyHits) {
        stage=Math.max(0,Math.min(9,savedStage));
        turkeyHits=Math.max(0,Math.min(3,savedTurkeyHits));
        towingPlow=(stage==2);
        endingShown=false;
        stormTimer=90f;
        px=x;
        pz=z;
        heading=h;
        vx=vz=0f;
        fuel=Math.max(0f,Math.min(100f,savedFuel));
        gameplay=true;
        listener.onDialogue("町内放送", "おかえりなさい。町はまだ雪の中です。特に改善していません。");
    }

    public void enterFreeRoam() {
        stage=9;
        gameplay=true;
        endingShown=true;
        towingPlow=false;
        saveNow();
        listener.onDialogue("ジム", "自由に走れ。トラックは引退しない。次の持ち主に移るだけだ。");
    }

    public void control(int which, boolean value) {
        if (which==0) left=value;
        if (which==1) right=value;
        if (which==2) brake=value;
        if (which==3) accel=value;
    }

    public void action() {
        actionRequested=true;
    }

    public void honk() {
        honkRequested=true;
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

        program=link(vs,fs);
        aPos=GLES20.glGetAttribLocation(program,"aPos");
        aNormal=GLES20.glGetAttribLocation(program,"aNormal");
        uMvp=GLES20.glGetUniformLocation(program,"uMvp");
        uModel=GLES20.glGetUniformLocation(program,"uModel");
        uColor=GLES20.glGetUniformLocation(program,"uColor");
        uLight=GLES20.glGetUniformLocation(program,"uLight");
        uFogColor=GLES20.glGetUniformLocation(program,"uFogColor");

        cube=makeCube();
        pyramid=makePyramid();
        lastNs=System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        Matrix.perspectiveM(projection,0,63f,(float)width/Math.max(1,height),.12f,560f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now=System.nanoTime();
        float dt=Math.min(.04f,(now-lastNs)/1_000_000_000f);
        lastNs=now;
        worldTime+=dt;

        if (gameplay) update(dt);
        else updateTitleWorld(dt);

        float daylight=.5f+.5f*(float)Math.sin(worldTime*.014f+.8f);
        float skyR=.30f+.31f*daylight;
        float skyG=.39f+.32f*daylight;
        float skyB=.50f+.31f*daylight;

        GLES20.glClearColor(skyR,skyG,skyB,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniform3f(uLight,-.38f,1f,.22f);
        GLES20.glUniform3f(uFogColor,skyR+.07f,skyG+.07f,skyB+.07f);

        if (gameplay) {
            float speed=speed();
            float camBack=11.2f+Math.min(5.5f,speed*.24f);
            float camX=px-(float)Math.sin(heading)*camBack;
            float camZ=pz+(float)Math.cos(heading)*camBack;
            float lookX=px+(float)Math.sin(heading)*6.2f;
            float lookZ=pz-(float)Math.cos(heading)*6.2f;
            Matrix.setLookAtM(view,0,camX,6.4f,camZ,lookX,1.4f,lookZ,0,1,0);
        } else {
            float a=worldTime*.075f;
            float cx=(float)Math.sin(a)*34f;
            float cz=-238f+(float)Math.cos(a)*34f;
            Matrix.setLookAtM(view,0,cx,15f,cz,0f,2.2f,-244f,0,1,0);
        }

        Matrix.multiplyMM(pv,0,projection,0,view,0);
        drawWorld(daylight);
        if (gameplay) {
            drawPlayerTruck();
            if (towingPlow) drawTowedPlow();
            drawMissionMarker();
        } else {
            parkedTruck(0,-231,.38f,.10f,.045f,0f);
        }
        drawTurkey();
        drawSnowflakes();
    }

    private void updateTitleWorld(float dt) {
        turkeyPhase+=dt;
        updateTurkey(dt);
    }

    private void update(float dt) {
        hudTimer-=dt;
        radioTimer-=dt;
        saveTimer-=dt;
        actionCooldown-=dt;
        honkCooldown-=dt;
        bumpCooldown-=dt;
        turkeyPhase+=dt;
        updateTurkey(dt);

        boolean road=onRoad(px,pz);
        float speed=speed();
        float steer=(left?-1f:0f)+(right?1f:0f);

        if (speed>.35f) {
            float steering=road ? .52f : .35f;
            heading+=steer*dt*(steering+Math.min(.85f,speed*.042f));
        }

        float fx=(float)Math.sin(heading);
        float fz=-(float)Math.cos(heading);

        if (accel && fuel>0f) {
            float force=road ? 9.2f : 5.0f;
            vx+=fx*force*dt;
            vz+=fz*force*dt;
            fuel=Math.max(0f,fuel-dt*.16f);
        }

        if (brake) {
            vx*=Math.max(0f,1f-dt*3.8f);
            vz*=Math.max(0f,1f-dt*3.8f);
        }

        float drag=road ? .52f : 1.35f;
        vx*=Math.max(0f,1f-dt*drag);
        vz*=Math.max(0f,1f-dt*drag);

        float forward=vx*fx+vz*fz;
        float align=road ? Math.min(1f,dt*3.0f) : Math.min(1f,dt*.75f);
        vx+=(fx*forward-vx)*align;
        vz+=(fz*forward-vz)*align;

        float max=road ? (towingPlow ? 15f : 23f) : 12f;
        speed=speed();
        if (speed>max) {
            vx*=max/speed;
            vz*=max/speed;
        }

        float oldX=px;
        float oldZ=pz;
        px+=vx*dt;
        pz+=vz*dt;

        if (hitsBuilding(px,pz)) {
            px=oldX;
            pz=oldZ;
            vx*=-.18f;
            vz*=-.18f;
            if (bumpCooldown<=0f) {
                bumpCooldown=3f;
                listener.onDialogue("車内", "ゴンッ。建物は雪と違って押しても動かない。");
            }
        }

        if (px<-59f || px>59f || pz>32f || pz<-625f) {
            px=Math.max(-58f,Math.min(58f,px));
            pz=Math.max(-620f,Math.min(31f,pz));
            vx*=-.12f;
            vz*=-.12f;
            if (bumpCooldown<=0f) {
                bumpCooldown=4f;
                listener.onDialogue("町内放送", "そこから先に道路はありません。地図にもありません。戻ってください。");
            }
        }

        if (honkRequested) {
            honkRequested=false;
            handleHonk();
        }
        if (actionRequested) {
            actionRequested=false;
            handleAction();
        }

        if (stage==8) {
            stormTimer-=dt;
            if (stormTimer<=0f) {
                stormTimer=90f;
                listener.onDialogue("メアリーの無線",
                        "時間切れ！……と言いたいけど予備の肉が山ほどある。もう一回来な。パイン・クリークでは失敗も大盛りだ。");
            }
        }

        if (radioTimer<=0f) {
            radioTimer=26f+random.nextFloat()*20f;
            String[] r=RADIO[random.nextInt(RADIO.length)];
            listener.onDialogue(r[0],r[1]);
        }

        if (fuel<=0f && ((int)worldTime)%12==0 && actionCooldown<=0f) {
            actionCooldown=5f;
            listener.onDialogue("車内", "エンジンが咳をして黙った。燃料計は今回は正しかったらしい。");
        }

        if (saveTimer<=0f) {
            saveTimer=4f;
            saveNow();
        }

        if (hudTimer<=0f) {
            hudTimer=.16f;
            int kmh=Math.round(speed()*4.25f);
            String roadText=onRoad(px,pz) ? "圧雪路" : "深雪";
            String location=locationName();
            String status="速度 "+kmh+" km/h\n燃料 "+Math.round(fuel)+"%　"+roadText+"\n現在地 "+location;
            String goal=goalText();
            if (nearTarget() && stage<9 && stage!=7) goal+="　【アクション可能】";
            String ch=chapterText();
            listener.onHud(status,goal,ch,.67f+Math.min(1.0f,kmh/90f),.10f+Math.min(.18f,kmh/360f));
        }
    }

    private void handleAction() {
        if (actionCooldown>0f) return;
        actionCooldown=1.2f;

        if (stage==9) {
            String[] free={
                    "メアリー「自由時間？ じゃあ食堂に来な。普通盛りはまだ残ってる。たぶん普通じゃないけど。」",
                    "ジム「暇なら18台目を探しに行くか？」",
                    "ボブ「何も壊れてない時こそ工具を買うんだ。」"
            };
            listener.onDialogue("住民",free[random.nextInt(free.length)]);
            return;
        }

        if (!nearTarget()) {
            listener.onDialogue("車内", "目的地はもう少し先だ。黄色い印の近くまで行こう。");
            return;
        }

        if (speed()>5.5f) {
            listener.onDialogue("車内", "まず止まろう。パイン・クリークでも会話しながら突っ込むのは失礼だ。");
            return;
        }

        switch(stage) {
            case 0:
                listener.onDialogue("メアリー（食堂）",
                        "BBQの荷物ありがと。『小盛り』を頼む客が来たから厨房がざわついてる。初めての冬？ 次は町長を見てきな。除雪車ごと埋まった。");
                stage=1;
                break;
            case 1:
                listener.onDialogue("町長",
                        "除雪車が埋まった。だから私のピックアップで除雪車を引く。行政手続き？ まず牽引ロープだ。公共事業ヤードまで頼む！");
                towingPlow=true;
                stage=2;
                break;
            case 2:
                listener.onDialogue("町長",
                        "救出成功。市の除雪車を市長の私物で救った。会計処理は春に考える。次は給油所のボブからジャンパーケーブルを受け取ってくれ。");
                towingPlow=false;
                stage=3;
                break;
            case 3:
                fuel=100f;
                listener.onDialogue("ボブ（給油所）",
                        "満タンとケーブルだ。寒さでバッテリーは死ぬ。住民はコーヒーを追加する。東の横道に一台『休憩中』のトラックがいる。");
                stage=4;
                break;
            case 4:
                listener.onDialogue("トラックの持ち主",
                        "助かった！ 故障じゃない、冬眠だ。四月まで寝かせる予定だった。ケーブルはボブへ返さなくていい。どうせ町中を巡回してるから。");
                stage=5;
                break;
            case 5:
                listener.onDialogue("ボブ（中古車店）",
                        "看板を読め。『走れば車だ』。走らない？ なら『将来性あり』だ。ジムがまた一台増やしたから見に行ってくれ。");
                stage=6;
                break;
            case 6:
                listener.onDialogue("ジム",
                        "17台目だ。壊れてない。まだ交換してない部品が残ってるだけだ。トラックは引退しない、持ち主が変わるだけだ。……ところで役場が七面鳥に占拠された。");
                stage=7;
                break;
            case 7:
                listener.onDialogue("町役場",
                        "ケビンはアクションボタンでは動きません。警笛を鳴らしてください。彼は行政手続きを理解しません。");
                break;
            case 8:
                listener.onDialogue("メアリー（食堂）",
                        "間に合った！ 雪嵐の夜にBBQを届けられたら、もう観光客じゃない。最後の質問だ。『初めての冬？ トラック買った？ 一回は埋まった？』……全部済んだね。");
                stage=9;
                saveNow();
                if (!endingShown) {
                    endingShown=true;
                    listener.onEnding();
                }
                return;
        }
        saveNow();
    }

    private void handleHonk() {
        if (honkCooldown>0f) return;
        honkCooldown=1.2f;

        if (stage==7 && dist(px,pz,turkeyX,turkeyZ)<18f) {
            turkeyHits++;
            if (turkeyHits==1) {
                listener.onDialogue("ケビン（七面鳥）", "ゴボゴボッ！！　警笛に抗議しつつ、役場の階段を一段だけ譲った。");
            } else if (turkeyHits==2) {
                listener.onDialogue("町長", "効いてる！ もう一回だ！ 議会より話が早い！");
            } else {
                listener.onDialogue("町役場",
                        "ケビン退去確認。職員一同より感謝します。なお明日また来る可能性は高いです。食堂へ急げ。雪嵐が来る。");
                stage=8;
                stormTimer=90f;
                saveNow();
            }
            return;
        }

        String[] lines={
                "一回なら挨拶。二回なら助けてくれ。三回ならブレーキが無い。",
                "警笛が元気ならまだ走れる。たぶん。",
                "誰かが手を振った。手袋が厚すぎて親指かどうかは分からない。"
        };
        listener.onDialogue("近くの住民",lines[random.nextInt(lines.length)]);
    }

    private boolean nearTarget() {
        if (stage>=9) return false;
        float tx=targetX();
        float tz=targetZ();
        return dist(px,pz,tx,tz)<13.5f;
    }

    private float targetX() {
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
            default: return 0f;
        }
    }

    private float targetZ() {
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
            default: return 0f;
        }
    }

    private String chapterText() {
        switch(stage) {
            case 0: return "序章　初めての冬";
            case 1: return "第1章　町長と除雪車";
            case 2: return "第1章　除雪車救出";
            case 3: return "第2章　バッテリーの町";
            case 4: return "第2章　四月まで休憩中";
            case 5: return "第3章　走れば車だ";
            case 6: return "第4章　17台目";
            case 7: return "第5章　役場のケビン";
            case 8: return "最終章　春まで";
            default: return "自由走行";
        }
    }

    private String goalText() {
        switch(stage) {
            case 0: return "巨大BBQの荷物を食堂へ届けろ";
            case 1: return "町役場で埋まった除雪車を確認";
            case 2: return "除雪車を公共事業ヤードまで牽引";
            case 3: return "給油所で燃料とジャンパーケーブルを受け取る";
            case 4: return "東の横道で冬眠中のトラックを救援";
            case 5: return "ボブ中古車店『走れば車だ』へ";
            case 6: return "ジムの17台目のトラックを確認";
            case 7: return "役場を占拠する七面鳥ケビンに警笛を3回　"+turkeyHits+"/3";
            case 8: return "雪嵐のBBQ緊急便　食堂へ戻れ　残り "+Math.max(0,(int)stormTimer)+"秒";
            default: return "町を自由に走って狂った日常を探せ";
        }
    }

    private String locationName() {
        if (dist(px,pz,-15,-108)<28) return "家族食堂";
        if (dist(px,pz,16,-244)<30) return "町役場";
        if (dist(px,pz,-42,-244)<25) return "公共事業ヤード";
        if (dist(px,pz,-16,-358)<30) return "給油所";
        if (dist(px,pz,-18,-486)<28) return "ボブ中古車店";
        if (dist(px,pz,19,-486)<28) return "ジムの庭";
        if (!onRoad(px,pz)) return "雪原";
        return "メイン通り";
    }

    private void saveNow() {
        listener.onSave(stage,px,pz,heading,fuel,turkeyHits);
    }

    private float speed() {
        return (float)Math.sqrt(vx*vx+vz*vz);
    }

    private boolean onRoad(float x,float z) {
        if (Math.abs(x)<=ROAD_HALF) return true;
        for(float cross:new float[]{-108f,-244f,-358f,-486f}) {
            if (Math.abs(z-cross)<=ROAD_HALF && Math.abs(x)<60f) return true;
        }
        return false;
    }

    private boolean hitsBuilding(float x,float z) {
        for(House h:houses) {
            if (Math.abs(x-h.x)<h.w*.5f+1.4f && Math.abs(z-h.z)<h.d*.5f+2.0f) return true;
        }
        return false;
    }

    private void updateTurkey(float dt) {
        float baseX=8f;
        float baseZ=-250f;
        turkeyX=baseX+(float)Math.sin(turkeyPhase*1.55f)*6.5f;
        turkeyZ=baseZ+(float)Math.cos(turkeyPhase*1.12f)*5f;
        if (stage==7 && dist(px,pz,turkeyX,turkeyZ)<7f) {
            turkeyPhase+=dt*3f;
        }
    }

    private void buildTown() {
        houses.add(new House(-15,-108,11,5.4f,9,.52f,.10f,.065f,1));
        houses.add(new House( 16,-244,13,7.5f,10,.65f,.65f,.61f,2));
        houses.add(new House(-16,-358,12,5.3f,9,.09f,.29f,.42f,3));
        houses.add(new House(-18,-486,12,5.1f,9,.50f,.18f,.07f,4));
        houses.add(new House( 19,-486,13,4.8f,10,.34f,.23f,.14f,5));
        houses.add(new House(-42,-244,12,4.2f,10,.23f,.30f,.33f,6));

        for(int i=0;i<26;i++) {
            float z=10-i*24f+random.nextFloat()*8f;
            float side=random.nextBoolean()?-1f:1f;
            float x=side*(15f+random.nextFloat()*16f);
            houses.add(new House(x,z,6.5f+random.nextFloat()*4f,
                    3.8f+random.nextFloat()*2f,5.5f+random.nextFloat()*3f,
                    .24f+random.nextFloat()*.30f,.12f+random.nextFloat()*.18f,
                    .08f+random.nextFloat()*.14f,0));
        }

        for(float z:new float[]{-108f,-244f,-358f,-486f}) {
            for(int j=-2;j<=2;j++) {
                if(j==0) continue;
                float x=j*24f;
                if (Math.abs(z+244f)<1 && j==-2) continue;
                houses.add(new House(x,z+(j%2==0?16f:-16f),
                        7f+random.nextFloat()*3f,4f+random.nextFloat()*2f,7f,
                        .25f+random.nextFloat()*.25f,.12f,.09f,0));
            }
        }

        for(int i=0;i<125;i++) {
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

        for(float z=-46;z>-610;z-=40f) {
            lamps.add(new Lamp(-9f,z));
            lamps.add(new Lamp(9f,z));
        }
    }

    private void drawWorld(float daylight) {
        box(0,-.55f,-300f,124f,1f,690f,.88f,.92f,.94f,0);
        box(0,.02f,-300f,13f,.12f,668f,.15f,.18f,.20f,0);

        for(float z:new float[]{-108f,-244f,-358f,-486f}) {
            box(0,.025f,z,118f,.13f,13f,.16f,.18f,.20f,0);
        }

        for(float z=20;z>-625;z-=13f) {
            box(0,.11f,z,.16f,.04f,4.3f,.94f,.72f,.11f,0);
        }
        for(float cz:new float[]{-108f,-244f,-358f,-486f}) {
            for(float x=-54;x<=54;x+=13f) {
                box(x,.11f,cz,4.3f,.04f,.16f,.94f,.72f,.11f,0);
            }
        }

        for(float z=20;z>-625;z-=17f) {
            box(-7.25f,.35f,z,1.1f,.7f,10f,.94f,.96f,.97f,0);
            box( 7.25f,.35f,z,1.1f,.7f,10f,.94f,.96f,.97f,0);
        }

        for(House h:houses) {
            if (dist(px,pz,h.x,h.z)<300f || !gameplay) drawHouse(h);
        }
        for(Tree t:trees) {
            if (dist(px,pz,t.x,t.z)<260f || !gameplay) drawTree(t);
        }
        for(Person p:people) {
            if (dist(px,pz,p.x,p.z)<180f || !gameplay) drawPerson(p);
        }

        boolean lampsOn=daylight<.36f;
        for(Lamp l:lamps) {
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

        // Used-car lot sign and public works snowplow.
        drawSign(-18,-475,.74f,.16f,.05f);
        drawSign(-42,-234,.24f,.31f,.34f);
        if (stage<2 || stage>2) {
            drawSnowPlow(12,-252,0f);
        }

        // Large snow drifts.
        for(int i=0;i<14;i++) {
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

        if(h.kind==1) {
            box(h.x,2.0f,front+.38f,h.w*.80f,.32f,1.25f,.76f,.10f,.065f,0);
        } else if(h.kind==2) {
            box(h.x-3.4f,2.3f,front+.8f,.6f,4.6f,.6f,.80f,.80f,.76f,0);
            box(h.x+3.4f,2.3f,front+.8f,.6f,4.6f,.6f,.80f,.80f,.76f,0);
        } else if(h.kind==3) {
            box(h.x,4.9f,h.z+6.2f,13f,.5f,7f,.87f,.87f,.84f,0);
            box(h.x-5f,2.4f,h.z+6.2f,.45f,4.8f,.45f,.72f,.72f,.70f,0);
            box(h.x+5f,2.4f,h.z+6.2f,.45f,4.8f,.45f,.72f,.72f,.70f,0);
            box(h.x-2.2f,.9f,h.z+6.2f,1.2f,1.8f,.8f,.70f,.08f,.06f,0);
            box(h.x+2.2f,.9f,h.z+6.2f,1.2f,1.8f,.8f,.70f,.08f,.06f,0);
        } else if(h.kind==4) {
            // used-car office awning
            box(h.x,3.0f,front+.45f,h.w*.92f,.35f,1.4f,.88f,.72f,.12f,0);
        } else if(h.kind==5) {
            // Jim's oversized garage door
            box(h.x,h.h*.48f,front+.05f,h.w*.55f,h.h*.62f,.08f,.20f,.20f,.18f,0);
        } else if(h.kind==6) {
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
        if(gameplay && dist(px,pz,x,z)>220f) return;
        box(x,1.0f,z,3.2f,1.15f,5.0f,r,g,b,rot);
        boxLocal(x,z,rot,0,1.85f,.62f,2.7f,1.15f,2.25f,r*.88f,g*.88f,b*.88f);
        boxLocal(x,z,rot,0,1.98f,-.56f,2.1f,.62f,.08f,.24f,.47f,.54f);
        for(float wx:new float[]{-1.55f,1.55f}) {
            for(float wz:new float[]{-1.55f,1.55f}) {
                boxLocal(x,z,rot,wx,.54f,wz,.55f,.55f,.50f,.022f,.022f,.022f);
            }
        }
    }

    private void drawPlayerTruck() {
        float r=.38f,g=.10f,b=.045f;
        boxLocal(px,pz,heading,0,1.0f,0,3.35f,1.25f,5.2f,r,g,b);
        boxLocal(px,pz,heading,0,1.92f,.65f,2.75f,1.30f,2.35f,.45f,.13f,.06f);
        boxLocal(px,pz,heading,0,2.06f,-.55f,2.25f,.70f,.08f,.18f,.45f,.53f);
        boxLocal(px,pz,heading,0,1.27f,-2.48f,2.3f,.25f,.09f,.72f,.12f,.05f);

        if(stage==0) {
            boxLocal(px,pz,heading,-.72f,1.58f,1.76f,1.0f,.8f,1.0f,.48f,.26f,.08f);
            boxLocal(px,pz,heading,.58f,1.55f,1.48f,1.0f,.75f,1.0f,.55f,.31f,.10f);
        }

        for(float wx:new float[]{-1.62f,1.62f}) {
            for(float wz:new float[]{-1.62f,1.62f}) {
                boxLocal(px,pz,heading,wx,.52f,wz,.62f,.62f,.52f,.022f,.022f,.022f);
            }
        }

        boxLocal(px,pz,heading,-.95f,1.03f,-2.62f,.48f,.28f,.08f,.96f,.86f,.48f);
        boxLocal(px,pz,heading,.95f,1.03f,-2.62f,.48f,.28f,.08f,.96f,.86f,.48f);
    }

    private void drawSnowPlow(float x,float z,float rot) {
        box(x,1.15f,z,4.0f,1.6f,6.0f,.72f,.46f,.08f,rot);
        boxLocal(x,z,rot,0,2.15f,.55f,3.1f,1.7f,2.7f,.78f,.53f,.10f);
        boxLocal(x,z,rot,0,.75f,-3.4f,6.0f,1.2f,.45f,.82f,.82f,.78f);
        for(float wx:new float[]{-1.9f,1.9f}) {
            for(float wz:new float[]{-1.9f,1.9f}) {
                boxLocal(x,z,rot,wx,.55f,wz,.72f,.72f,.60f,.025f,.025f,.025f);
            }
        }
    }

    private void drawTowedPlow() {
        float back=8.2f;
        float tx=px-(float)Math.sin(heading)*back;
        float tz=pz+(float)Math.cos(heading)*back;
        drawSnowPlow(tx,tz,heading);
        for(int i=1;i<=4;i++) {
            float t=i/5f;
            float rx=px+(tx-px)*t;
            float rz=pz+(tz-pz)*t;
            box(rx,.55f,rz,.10f,.10f,1.2f,.18f,.12f,.07f,heading);
        }
    }

    private void drawMissionMarker() {
        if(stage>=9) return;
        float tx=targetX();
        float tz=targetZ();
        if(dist(px,pz,tx,tz)>320f) return;

        float bob=4.2f+(float)Math.sin(worldTime*2.5f)*.65f;
        pyramid(tx,bob,tz,2.5f,3.6f,2.5f,.96f,.68f,.04f,worldTime*.8f);
        box(tx,.07f,tz,4.8f,.09f,4.8f,.86f,.54f,.04f,worldTime*.45f);
    }

    private void drawTurkey() {
        if(gameplay && dist(px,pz,turkeyX,turkeyZ)>190f) return;
        box(turkeyX,.78f,turkeyZ,.92f,1.15f,.78f,.38f,.21f,.09f,turkeyPhase);
        box(turkeyX,1.48f,turkeyZ-.18f,.42f,.42f,.42f,.59f,.10f,.065f,turkeyPhase);
        for(int i=-2;i<=2;i++) {
            float a=turkeyPhase+i*.38f;
            float fx=turkeyX+(float)Math.sin(a)*.68f;
            float fz=turkeyZ+(float)Math.cos(a)*.68f;
            box(fx,.98f,fz,.23f,1.12f,.12f,.46f,.10f+.07f*(i+2),.06f,a);
        }
    }

    private void drawSnowflakes() {
        float ox=gameplay?px:0f;
        float oz=gameplay?pz:-244f;
        for(int i=0;i<48;i++) {
            float a=i*1.71f+worldTime*.17f;
            float radius=8f+(i%10)*3.4f;
            float sx=ox+(float)Math.sin(a)*radius;
            float sz=oz+(float)Math.cos(a*.83f)*radius;
            float sy=.8f+positiveMod(i*1.83f-worldTime*2.8f,8f);
            box(sx,sy,sz,.065f,.065f,.065f,.97f,.98f,1f,0);
        }
    }

    private static float positiveMod(float v,float m) {
        float r=v%m;
        return r<0?r+m:r;
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
                     float r,float g,float b,float rot) {
        drawMesh(cube,x,y,z,sx,sy,sz,r,g,b,rot);
    }

    private void pyramid(float x,float y,float z,float sx,float sy,float sz,
                         float r,float g,float b,float rot) {
        drawMesh(pyramid,x,y,z,sx,sy,sz,r,g,b,rot);
    }

    private void drawMesh(FloatBuffer mesh,float x,float y,float z,
                          float sx,float sy,float sz,float r,float g,float b,float rot) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.rotateM(model,0,(float)Math.toDegrees(rot),0,1,0);
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
        for(int i:idx) {
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

    private void tri(ArrayList<Float> out,float[] a,float[] b,float[] c) {
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy;
        float ny=uz*vx-ux*vz;
        float nz=ux*vy-uy*vx;
        float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        nx/=len; ny/=len; nz/=len;
        for(float[] p:new float[][]{a,b,c}) {
            out.add(p[0]); out.add(p[1]); out.add(p[2]);
            out.add(nx); out.add(ny); out.add(nz);
        }
    }

    private FloatBuffer toBuffer(ArrayList<Float> src) {
        FloatBuffer b=ByteBuffer.allocateDirect(src.size()*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        for(float f:src) b.put(f);
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

    private static float dist(float ax,float az,float bx,float bz) {
        float dx=ax-bx;
        float dz=az-bz;
        return (float)Math.sqrt(dx*dx+dz*dz);
    }
}
