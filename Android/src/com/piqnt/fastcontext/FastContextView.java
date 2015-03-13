package com.piqnt.fastcontext;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import com.adobe.plugins.FastCanvasJNI;
import com.adobe.plugins.FastCanvasTextureDimension;

public class FastContextView extends GLSurfaceView {

    private FastContextPlugin plugin;
    private LinkedList<Command> queue = new LinkedList<Command>();
    private Map<Integer, Texture> textures = new HashMap<Integer, Texture>();
    private String renderCommand;

    public FastContextView(Context context, FastContextPlugin plugin) {
        super(context);
        this.plugin = plugin;
        this.setEGLConfigChooser(false);// turn off the depth buffer
        this.setRenderer(new FastContextRenderer());
        this.setRenderMode(RENDERMODE_CONTINUOUSLY);

        this.setFocusableInTouchMode(true);
        this.requestFocus();
    }

    // @Override
    // public boolean onKeyDown(int keyCode, KeyEvent event) {
    // return plugin.webView.onKeyDown(keyCode, event);
    // }
    // @Override
    // public boolean onKeyUp(int keyCode, KeyEvent event) {
    // return plugin.webView.onKeyUp(keyCode, event);
    // }
    // @Override
    // public boolean onTouchEvent(MotionEvent event) {
    // return plugin.webView.dispatchTouchEvent(event);
    // }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        FastCanvasJNI.contextLost();
        super.onPause();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        onResume();
        super.surfaceCreated(holder);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.i(TAG, "surfaceChanged");
        super.surfaceChanged(holder, format, w, h);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        onPause();
        FastCanvasJNI.release();
        super.surfaceDestroyed(holder);
    }

    public class FastContextRenderer implements GLSurfaceView.Renderer {

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.i(TAG, "onSurfaceCreated: " + gl + ", " + config);

            IntBuffer ib = IntBuffer.allocate(100);
            ib.position(0);
            GLES10.glGetIntegerv(GLES10.GL_RED_BITS, ib);
            int red = ib.get(0);
            GLES10.glGetIntegerv(GLES10.GL_GREEN_BITS, ib);
            int green = ib.get(0);
            GLES10.glGetIntegerv(GLES10.GL_BLUE_BITS, ib);
            int blue = ib.get(0);
            GLES10.glGetIntegerv(GLES10.GL_STENCIL_BITS, ib);
            int stencil = ib.get(0);
            GLES10.glGetIntegerv(GLES10.GL_DEPTH_BITS, ib);
            int depth = ib.get(0);

            Log.i(TAG, "onSurfaceCreated R: " + red + " G: " + green + " B: "
                    + blue + " DEPETH: " + depth + " STENCIL: " + stencil);

            for (final Texture texture : textures.values()) {
                Log.i(TAG, "queue reload texture: " + texture);
                queue.offer(new Command() {
                    @Override
                    public void exec() {
                        loadTexture(texture, null);
                    }
                });
            }
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.i(TAG, "onSurfaceChanged");
            FastCanvasJNI.surfaceChanged(width, height);
        }

        public void onDrawFrame(GL10 gl) {
            if (!queue.isEmpty()) {
                Log.i(TAG, "dump command queue");
                Command cmd;
                while ((cmd = queue.poll()) != null) {
                    try {
                        cmd.exec();
                    } catch (Exception e) {
                        Log.e(TAG, "", e);
                    }
                }
            }

            if (renderCommand != null) {
                FastCanvasJNI.render(renderCommand);
                checkError();
            }
        }

        private static final String TAG = "FastContextRenderer";
    }

    private void unloadTexture(int id) {
        Log.i(TAG, "unload texture: " + id);
        Texture old = textures.get(id);
        if (old != null && old.loaded) {
            Log.i(TAG, "unload texture: " + old);
            old.unload();
            textures.remove(id);
        }
    }

    private void loadTexture(Texture texture, CallbackContext callback) {

        Log.i(TAG, "load texture: " + texture);

        unloadTexture(texture.id);

        try {
            FastCanvasTextureDimension dim = texture.load();
            textures.put(texture.id, texture);

            Log.i(TAG, "loaded texture: " + texture);

            if (callback != null) {
                JSONArray args = new JSONArray();
                args.put(dim.width);
                args.put(dim.height);
                callback.success(args);
            }

        } catch (Exception e) {
            Log.i(TAG, "load texture error: ", e);
            if (callback != null) {
                callback.error(e.getMessage());
            }
        }
    }

    interface Command {
        void exec();
    }

    class Texture {
        public final String url;
        public final int id;
        public boolean loaded;

        public Texture(String url, int id) {
            this.url = url;
            this.id = id;
        }

        @Override
        public String toString() {
            return url + "#" + id + "@" + hashCode();
        }

        private void unload() {
            FastCanvasJNI.removeTexture(this.id);
            this.loaded = false;
            checkError();
        }

        private FastCanvasTextureDimension load() throws IOException {

            FastCanvasTextureDimension dim = new FastCanvasTextureDimension();

            String path = "www/" + this.url;

            AssetManager assets = getContext().getAssets();

            // PNG files with premultiplied
            // alpha and GLUtils don't get along
            // http://stackoverflow.com/questions/3921685
            if (path.toLowerCase(Locale.US).endsWith(".png")) {
                if (FastCanvasJNI.addPngTexture(assets, path, this.id, dim)) {
                    this.loaded = true;
                    return dim;
                }
                Log.i(TAG, "native PNG load filaed, falling back to GLUtils");
            }

            Bitmap bmp = BitmapFactory.decodeStream(assets.open(path));

            int[] glID = new int[1];
            GLES10.glGenTextures(1, glID, 0);
            GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, glID[0]);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
                    GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
                    GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);

            int width = bmp.getWidth(), height = bmp.getHeight();
            int p2width = 2, p2height = 2;
            while (p2width < width)
                p2width *= 2;
            while (p2height < height)
                p2height *= 2;

            if (width == p2width && height == p2height) {
                GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bmp, 0);
            } else {
                Log.i(TAG, "load texture scaling texture " + this
                        + " to power of 2");
                GLES10.glTexImage2D(GLES10.GL_TEXTURE_2D, 0, GLES10.GL_RGBA,
                        p2width, p2height, 0, GLES10.GL_RGBA,
                        GLES10.GL_UNSIGNED_BYTE, null);
                GLUtils.texSubImage2D(GLES10.GL_TEXTURE_2D, 0, 0, 0, bmp);
                width = p2width;
                height = p2height;
            }

            checkError();

            FastCanvasJNI.addTexture(this.id, glID[0], width, height);
            Log.i(TAG, "load texture done: " + this);

            dim.width = bmp.getWidth();
            dim.height = bmp.getHeight();

            this.loaded = true;
            return dim;
        }

        private static final String TAG = "FastContextTexture";
    }

    public boolean execute(String action, JSONArray args,
            final CallbackContext callbackContext) throws JSONException {
        // Log.i(TAG, "execute: " + action);

        try {

            if (action.equals("render")) {
                renderCommand = args.getString(0);
                return true;

            } else if (action.equals("setBackgroundColor")) {
                String color = args.getString(0);
                Log.i(TAG, "setBackground: " + color);
                try {
                    int red = Integer.valueOf(color.substring(0, 2), 16);
                    int green = Integer.valueOf(color.substring(2, 4), 16);
                    int blue = Integer.valueOf(color.substring(4, 6), 16);
                    FastCanvasJNI.setBackgroundColor(red, green, blue);
                } catch (Exception e) {
                    Log.e(TAG, "Invalid background color: " + color, e);
                }

                return true;

            } else if (action.equals("loadTexture")) {
                final Texture texture = new Texture(args.getString(0),
                        args.getInt(1));
                Log.i(TAG, "loadTexture " + texture);
                queue.offer(new Command() {
                    @Override
                    public void exec() {
                        loadTexture(texture, callbackContext);
                    }
                });
                return true;

            } else if (action.equals("unloadTexture")) {
                Log.i(TAG, "unload texture");
                int id = args.getInt(0);
                unloadTexture(id);

                return true;

            } else if (action.equals("setOrtho")) {
                int width = args.getInt(0);
                int height = args.getInt(1);

                Log.i(TAG, "setOrtho: " + width + ", " + height);
                FastCanvasJNI.setOrtho(width, height);

                return true;

            } else if (action.equals("capture")) {
                Log.i(TAG, "capture");

                // set the root path to /mnt/sdcard/
                String file = Environment.getExternalStorageDirectory()
                        + args.getString(4);
                File dir = new File(file).getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    callbackContext.sendPluginResult(new PluginResult(
                            PluginResult.Status.ERROR,
                            "Could not create directory"));
                    return true;
                }

                int x = args.optInt(0, 0);
                int y = args.optInt(1, 0);
                int width = args.optInt(2, -1);
                int height = args.optInt(3, -1);

                FastCanvasJNI.captureGLLayer(callbackContext.getCallbackId(),
                        x, y, width, height, file);

                return true;

            } else if (action.equals("isAvailable")) {
                callbackContext.sendPluginResult(new PluginResult(
                        PluginResult.Status.OK, true));
                return true;

            } else {
                Log.i(TAG, "invalid execute action: " + action);
            }

        } catch (Exception e) {
            Log.e(TAG, "error executing action: " + action + "(" + args + ")",
                    e);
        }

        return false;
    }

    private void checkError() {
        int error = GLES10.glGetError();
        if (error != GLES10.GL_NO_ERROR) {
            Log.i(TAG, "glError=" + error);
        }
        assert error == GLES10.GL_NO_ERROR;
    }

    private static final String TAG = "FastContextView";
}