package com.piqnt.fastcontext;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewClient;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class FastContextPlugin extends CordovaPlugin {

    CordovaWebView webView;
    FastContextView fastView;

    public static final boolean TOP = true;

    @Override
    public void initialize(final CordovaInterface cordova,
            final CordovaWebView webView) {
        Log.i(TAG, "initialize");
        super.initialize(cordova, webView);

        final Activity activity = cordova.getActivity();

        this.fastView = new FastContextView(activity, this);
        this.webView = webView;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (FastContextPlugin.TOP) {
                    // glsurface over webview
                    webView.addView(fastView);
                    return;
                }

                // webview over glsurface
                // 1. transparent webview doesn't work on some devices, 4.0.3
                // 2. some plugins assume webview parent to be linearlayout and
                // add themselve to the parent, admob-plugin-pro for example

                ViewGroup webParent = (ViewGroup) webView.getParent();

                FrameLayout newLayout = new FrameLayout(activity);
                newLayout.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

                webParent.removeView(webView);
                webParent.addView(newLayout);

                newLayout.addView(fastView);
                newLayout.addView(webView);

                webView.setBackgroundColor(Color.TRANSPARENT);
                webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                webView.setWebViewClient(new CordovaWebViewClient(cordova,
                        webView) {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        webView.setBackgroundColor(Color.TRANSPARENT);
                        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                    }
                });
            }
        });
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.i(TAG, "onResume");
        // fastView.onResume();
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.i(TAG, "onPause");
        // fastView.onPause();
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

    public static void executeCallback(String callbackID, boolean isError,
            String result) {
        // TODO:
        // Status status = isError ? Status.ERROR : Status.OK;
        // if (instance != null) {
        // instance.webView.sendPluginResult(new PluginResult(status, result),
        // callbackID);
        // }
    }

    protected static final String TAG = "FastContextPlugin";
}
