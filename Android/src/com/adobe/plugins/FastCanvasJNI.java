package com.adobe.plugins;

public class FastCanvasJNI {
    // Native methods

    public static native void setBackgroundColor(int red, int green, int blue);

    public static native void setOrtho(int width, int height);

    // id's must be from 0 to numTextures-1
    public static native void addTexture(int id, int glID, int width, int height);

    // id's must be from 0 to numTextures-1
    public static native boolean addPngTexture(Object mgr, String path, int id,
            FastCanvasTextureDimension dim);

    // id must have been passed to addTexture in the past
    public static native void removeTexture(int id);

    public static native void render(String renderCommands);

    public static native void surfaceChanged(int width, int height);

    // captures the current contents of the GL layer and writes to a temporary
    // file
    public static native void captureGLLayer(String callbackID, int x, int y,
            int width, int height, String fileName);

    // Deletes native memory associated with lost GL context
    public static native void contextLost();

    // Deletes native canvas
    public static native void release();

    // Load library
    static {
        System.loadLibrary("FastCanvasJNI");
    }
}
