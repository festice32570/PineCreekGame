package com.pinecreek.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

public class AudioEngine {
    private static final int RATE = 22050;
    private AudioTrack bgm;
    private AudioTrack engine;
    private AudioTrack wind;
    private final short[] hornPcm;
    private final short[] eventPcm;
    private boolean started;
    private boolean paused;
    private boolean menuMode = true;
    private float enginePitch = .75f;
    private float engineVolume = .12f;

    public AudioEngine() {
        hornPcm = makeHorn();
        eventPcm = makeEvent();
        try {
            bgm = loopTrack(makeBgm(16f));
            engine = loopTrack(makeEngine(1f));
            wind = loopTrack(makeWind(4f));
        } catch (Exception ignored) {
            bgm = engine = wind = null;
        }
    }

    public void start() {
        if (started) return;
        started = true;
        try {
            if (bgm != null) {
                bgm.setVolume(.30f);
                bgm.play();
            }
            if (wind != null) {
                wind.setVolume(.12f);
                wind.play();
            }
            if (engine != null) {
                engine.setVolume(.03f);
                engine.play();
            }
        } catch (Exception ignored) { }
    }

    public void setMenuMode(boolean menu) {
        menuMode = menu;
        try {
            if (bgm != null) bgm.setVolume(menu ? .34f : .25f);
            if (wind != null) wind.setVolume(menu ? .10f : .15f);
            if (engine != null) engine.setVolume(menu ? .02f : engineVolume);
        } catch (Exception ignored) { }
    }

    public void setEngine(float pitch, float volume) {
        enginePitch = Math.max(.55f, Math.min(1.75f, pitch));
        engineVolume = Math.max(.04f, Math.min(.30f, volume));
        if (engine == null) return;
        try {
            int hz = (int)(RATE * enginePitch);
            hz = Math.max(12000, Math.min(42000, hz));
            engine.setPlaybackRate(hz);
            engine.setVolume(menuMode ? .02f : engineVolume);
        } catch (Exception ignored) { }
    }

    public void horn() {
        oneShot(hornPcm, .68f);
    }

    public void event() {
        oneShot(eventPcm, .40f);
    }

    public void pause() {
        paused = true;
        try {
            if (bgm != null) bgm.pause();
            if (wind != null) wind.pause();
            if (engine != null) engine.pause();
        } catch (Exception ignored) { }
    }

    public void resume() {
        if (!started || !paused) return;
        paused = false;
        try {
            if (bgm != null) bgm.play();
            if (wind != null) wind.play();
            if (engine != null) engine.play();
            setMenuMode(menuMode);
            setEngine(enginePitch, engineVolume);
        } catch (Exception ignored) { }
    }

    public void release() {
        try { if (bgm != null) bgm.release(); } catch (Exception ignored) { }
        try { if (wind != null) wind.release(); } catch (Exception ignored) { }
        try { if (engine != null) engine.release(); } catch (Exception ignored) { }
        bgm = engine = wind = null;
    }

    private AudioTrack loopTrack(short[] pcm) {
        AudioTrack t = createStatic(pcm);
        t.setLoopPoints(0, pcm.length, -1);
        return t;
    }

    private AudioTrack createStatic(short[] pcm) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat fmt = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(fmt)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
        t.write(pcm, 0, pcm.length);
        return t;
    }

    private void oneShot(short[] pcm, float volume) {
        try {
            final AudioTrack t = createStatic(pcm);
            t.setVolume(volume);
            t.play();
            long ms = Math.max(250, pcm.length * 1000L / RATE + 150);
            new Thread(() -> {
                try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
                try { t.stop(); } catch (Exception ignored) { }
                try { t.release(); } catch (Exception ignored) { }
            }, "PineCreekSound").start();
        } catch (Exception ignored) { }
    }

    private short[] makeEngine(float seconds) {
        int n = (int)(RATE * seconds);
        short[] out = new short[n];
        for (int i=0; i<n; i++) {
            double t = (double)i / RATE;
            double wobble = .82 + .18 * Math.sin(2*Math.PI*2.15*t);
            double grit = pseudoNoise(i) * .14;
            double s = (
                    Math.sin(2*Math.PI*46*t)
                    + .48*Math.sin(2*Math.PI*92*t)
                    + .22*Math.sin(2*Math.PI*138*t)
                    + grit) * wobble * .22;
            out[i] = pcm(s);
        }
        return out;
    }

    private short[] makeWind(float seconds) {
        int n = (int)(RATE * seconds);
        short[] out = new short[n];
        double smooth = 0;
        for (int i=0; i<n; i++) {
            double t=(double)i/RATE;
            smooth = smooth*.985 + pseudoNoise(i*17+91)*.015;
            double gust=.45+.55*(.5+.5*Math.sin(2*Math.PI*.10*t));
            double whistle=.035*Math.sin(2*Math.PI*(380+24*Math.sin(2*Math.PI*.07*t))*t);
            out[i]=pcm((smooth*.32*gust)+whistle);
        }
        return out;
    }

    private short[] makeHorn() {
        int n=(int)(RATE*.72f);
        short[] out=new short[n];
        for(int i=0;i<n;i++){
            double t=(double)i/RATE;
            double attack=Math.min(1,t/.035);
            double release=Math.min(1,Math.max(0,(.72-t)/.10));
            double env=attack*release;
            double s=.34*Math.sin(2*Math.PI*312*t)+.24*Math.sin(2*Math.PI*416*t)+.08*Math.sin(2*Math.PI*624*t);
            out[i]=pcm(s*env);
        }
        return out;
    }

    private short[] makeEvent() {
        int n=(int)(RATE*.42f);
        short[] out=new short[n];
        for(int i=0;i<n;i++){
            double t=(double)i/RATE;
            double env=Math.exp(-7*t);
            double s=.34*Math.sin(2*Math.PI*(690+420*t)*t)+.12*Math.sin(2*Math.PI*1035*t);
            out[i]=pcm(s*env);
        }
        return out;
    }

    private short[] makeBgm(float seconds) {
        int n=(int)(RATE*seconds);
        short[] out=new short[n];
        double bpm=104.0;
        double beat=60.0/bpm;
        int[] chord={52,55,59,55,50,54,57,54,48,52,55,52,50,54,57,54};
        int[] melody={64,67,71,69,67,64,62,64,67,69,71,74,71,69,67,64};
        for(int i=0;i<n;i++){
            double t=(double)i/RATE;
            double s=0;

            double beatPos=(t/beat)%1.0;
            double kickEnv=Math.exp(-beatPos*13);
            s += .11*kickEnv*Math.sin(2*Math.PI*58*t);

            double half=(t%(beat*2));
            if(half>beat){
                double sn=(half-beat)/beat;
                if(sn<.24) s += .032*pseudoNoise(i*11+3)*Math.exp(-sn*14);
            }

            double hatPos=(t%(beat/2))/(beat/2);
            s += .012*pseudoNoise(i*29+5)*Math.exp(-hatPos*19);

            int ci=((int)(t/(beat*2)))%chord.length;
            s += .10*Math.sin(2*Math.PI*freq(chord[ci]-12)*t);

            double step=beat/2;
            int pi=((int)(t/step))%chord.length;
            double pt=t%step;
            double pe=Math.exp(-pt*5.5);
            double f=freq(chord[pi]);
            s += pe*(.055*Math.sin(2*Math.PI*f*t)+.021*Math.sin(2*Math.PI*f*2*t));

            int mi=((int)(t/beat))%melody.length;
            double mt=t%beat;
            if(mi%4!=3){
                double me=Math.exp(-mt*3.2);
                s += me*(.045*Math.sin(2*Math.PI*freq(melody[mi])*t)
                        +.018*Math.sin(2*Math.PI*freq(melody[mi]+12)*t));
            }
            out[i]=pcm(s);
        }
        return out;
    }

    private static double freq(int midi) {
        return 440.0 * Math.pow(2.0, (midi-69)/12.0);
    }

    private static double pseudoNoise(int x) {
        long n=x;
        n=(n<<13)^n;
        long nn=(n*(n*n*15731L+789221L)+1376312589L)&0x7fffffffL;
        return 1.0-(double)nn/1073741824.0;
    }

    private static short pcm(double v) {
        v=Math.max(-1,Math.min(1,v));
        return (short)(v*32767);
    }
}
