package com.pinecreek.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;

@SuppressLint("ViewConstructor")
public class GameSurface extends GLSurfaceView {
    public final GameRenderer renderer;

    public GameSurface(Context context, GameRenderer.Listener listener) {
        super(context);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        renderer = new GameRenderer(listener);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}

[executed on device: festice-virtual-machine (07fc5208-706b-4ca8-850a-ef91db884468)]