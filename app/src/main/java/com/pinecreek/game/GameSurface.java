package com.pinecreek.game;

import android.content.Context;
import android.opengl.GLSurfaceView;

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
