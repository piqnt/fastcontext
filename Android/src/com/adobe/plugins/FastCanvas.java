package com.adobe.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class FastCanvas extends CordovaPlugin {

    CordovaWebView webView;
    FastCanvasView fastView;

    @Override
    public void initialize(CordovaInterface cordova,
            final CordovaWebView webView) {
        Log.i(TAG, "initialize");
        super.initialize(cordova, webView);

        final Activity activity = cordova.getActivity();

        this.fastView = new FastCanvasView(activity, this);
        this.webView = webView;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FrameLayout layout = new FrameLayout(activity);
                layout.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                activity.setContentView(layout);

                layout.addView(fastView);

                // webview over glsurface
                // it doesn't work on some devices (4.0.3, etc)
                if (Boolean.FALSE) {
                    View cordova = (View) webView.getParent();
                    cordova.setBackgroundColor(0x00000000);
                    cordova.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                    layout.addView(cordova);

                    webView.setBackgroundColor(0x00000000);
                    webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            webView.setBackgroundColor(0x00000000);
                            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE,
                                    null);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.i(TAG, "onResume");
        // fastView.onResume();
        super.onResume(multitasking);
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.i(TAG, "onPause");
        // fastView.onPause();
        super.onPause(multitasking);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        return fastView.execute(action, args, callbackContext);
    }

    protected static final String TAG = "FastCanvasPlugin";
}
