# FastCanvas

FastCanvas is a Cordova/PhoneGap plugin which implements a very fast, 2D,
mostly-Canvas-compatible rendering surface for Android. It focuses on moving
graphics around on the screen quickly and efficiently using hardware
acceleration.

Unlike the HTML5 Canvas, FastCanvas will encompass your entire screen and cannot be
integrated with other elements in the DOM.  It lives outside of the DOM in a separate
rendering surface and covers HTML content.  More on how FastCanvas is displayed
to the screen is available in the [Architecture](#architecture) section.
If you already have an application which uses a full screen DOM Canvas, switching over
to FastCanvas could be an easy way to provide a boost in performance.

While FastCanvas attempts to look and behave very similar to the HTML5 Canvas, it
only supports a subset of the HTML5 Canvas API, focusing on what benefits most from
hardware acceleration.  More information about API support is described in the
[API](#api) section.


### Requirement

Your application must be designed so that:

1. Your application runs a full screen Canvas
2. Use of the 2D Canvas API is limited to transforms and drawing images (see [API](#api))

For devices that don't support FastCanvas, the API will fallback to using the standard
HTML Canvas.  This is handled for you seamlessly making it easy to write cross-platform
Canvas applications to use FastCanvas on Android.


### Installation

Use Cordova/PhoneGap [CLI](http://docs.phonegap.com/en/3.0.0/guide_cli_index.md.html)
to install FastCanvas:

```
	cordova plugin add https://github.com/piqnt/phonegap-plugin-fast-canvas.git
```

### Usage

FastCanvas was designed to mimic the standard HTML 2D Canvas to help make it easy to use
or implement in your existing Canvas-based applications.  Like the HTML Canvas, FastCanvas
consists of both a Canvas object and a context object.  The API is very similar with a few
exceptions:

* Canvas objects are created with `FastCanvas.create()`
* Image objects are created with `FastCanvas.createImage()`
* At the end of each frame a call to `FastCanvas.render()` is required to flush buffered API calls

These commands work with both FastCanvas and HTML Canvas applications.  Once you have your
application working with FastCanvas, it will also work with an HTML Canvas.

The following is a basic usage example of FastCanvas shifting and rotating an
image drawn to the screen:

```javascript
var canvas = FastCanvas.create(); // specific to FastCanvas
var context = canvas.getContext("2d");
var myImage = FastCanvas.createImage(); // specific to FastCanvas
myImage.onload = function(){
   context.translate(100, 100);
   context.rotate(Math.PI);
   context.drawImage(myImage, 0, 0);
   FastCanvas.render(); // specific to FastCanvas
}
myImage.src = "images/myImage.jpg";
```

You can see much of the code matches usage of the HTML Canvas.  `FastCanvas.create()` will even create
an HTML Canvas if FastCanvas is not supported.  This is also the case
with `FastCanvas.createImage()` which, depending on the result from `FastCanvas.create()` will create
a standard HTML Image if an HTML Canvas was created and a FastCanvasImage object (images used by FastCanvas)
when FastCanvas is being used.  Usage of these image objects with FastCanvas are largely the same as HTML Images,
as you can see from the code used to load the image above.  Similarly, the API of
the context returned from `FastCanvas.getContext("2d")` matches that of HTML's CanvasRenderingContext2D object,
as far as supported by FastCanvas. `FastCanvas.render()` is an extra step specific
to FastCanvas but will only do the necessary render step when a FastCanvas is being used and will
do nothing when an HTML Canvas has been created.

In addition to the code changes, because FastCanvas applications are full screen, your HTML should also include
the following meta tag to be assured that window metrics are reported accurately and consistently:

```html
   <meta name="viewport" content="user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, width=device-width, height=device-height" />
```



### Example

The game "Hungry Hero" has been re-implemented in JavaScript (from
AS3). It is a simple touch based flying game based on the Starling API.
We are currently using an approximation of Starling - a simple
display list API - to wrap calls to the FastCanvas plugin to do rendering.

https://github.com/phonegap/phonegap-fast-canvas-example


### API

The bulk of the FastCanvas API consists a subset of the methods within the
standard HTML5 Canvas implementation of
[CanvasRenderingContext2D](http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html#canvasrenderingcontext2d) -
the 2D context obtained when using `canvas.getContext("2d");`.  A goal in
designing FastCanvas was to make it as close as possible to the existing
HTML5 Canvas to make its easy and intuitive to use, and to make the
process of porting an existing application to FastCanvas as painless as possible.

The subset supported in FastCanvas for its context includes:

| Member | Notes |
| ------ | ----- |
| context.clearRect(); | Not supported, but available (NOOP) |
| context.drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh); | Supported |
| context.globalAlpha; | Supported |
| context.resetTransform(); | Supported |
| context.restore(); | Supported |
| context.rotate(angle); | Supported |
| context.save(); | Supported |
| context.scale(x, y); | Supported |
| context.setTransform(a, b, c, d, e, f); | Supported |
| context.transform(a, b, c, d, e, f); | Supported |
| context.translate(x, y); | Supported |

Additionally, FastCanvas includes the following:

| Member | Notes |
| ------ | ----- |
| FastCanvas.create(); | Creates a Canvas object for you, FastCanvas if available, otherwise a standard HTML Canvas. |
| FastCanvas.createImage(); | Creates an image object for you, FastCanvasImage if a FastCanvas was created in FastCanvas.create(), otherwise a standard HTML Image. |
| FastCanvas.render(); | To be called after all context calls are finished to commit the drawing to the screen. |
| FastCanvas.setBackgroundColor(color); | Sets the Canvas background (automatic for first time calling getContext()) |
| FastContext2D.capture(x, y, w, h, fileName, successCallback,  errorCallback); | Saves the current state of the Canvas as an image |


### Architecture

#### On A Top Surface

FastCanvas output will cover any HTML output - so you should generally avoid HTML rendering as it will only consume performance.
The FastCanvas plugin creates an OpenGL surface that sits on top of the browser.

#### OpenGL renderer in C++

The renderer itself is OpenGL ES1.1 command streams, and the code
is written in C++. The advantage of C++ is both portability and
control of memory management.

#### Separate Thread

Your JS code runs in the browser thread, while most of the work
FastCanvas does in in the UI thread. A tight stream of rendering commands
is sent between the threads. This allows some load balancing between
the threads, separation of the game from the renderer, and (in the
future) downclocking the render thread.


### Using FastCanvas Efficiently

For best performance, minimize the number of draw calls per fram in the GL layer.

What that means at the JavaScript level is:
* Use sprite sheets
* Use as few textures as possible
* Avoid swapping textures in and out, and preload if possible.
* Try to batch drawImage calls that use the same texture. It is vastly more efficient to make ten drawImage calls in a row using one texture, and then make ten more using a second texture, than to switch back and forth twenty times.

### Changing the Java interface

The native interface used by FastCanvas is defined in `FastCanvasJNI.java`.
If at any point in time you change `FastCanvasJNI.java`, you'll also need to regenerate
`FastCanvasJNI.h` and then recompile the native code.

To build changes [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html) is required.

These instructions are for Windows. If working on a Mac, use / instead of \, mv instead of move, etc.

1. Compile FastCanvasJNI.java:
```
  \FastCanvas\Android>javac -d . src\com\adobe\plugins\FastCanvasJNI.java src\com\adobe\plugins\FastCanvasTextureDimension.java
```

2. Create FastCanvasJNI.h
```
  \FastCanvas\Android>javah -jni com.adobe.plugins.FastCanvasJNI
```

3. Move it to the correct location
```
  \FastCanvas\Android>move com_adobe_plugins_FastCanvasJNI.h jni\FastCanvasJNI.h
```

4. Clean up
```
  \FastCanvas\Android>rmdir /s com
```

5. Build the JNI library from the command line (Command Prompt or Terminal window):
```
  \FastCanvas\Android>[path to NDK]\ndk-build
```

  Should produce output similar to:
```
    Compile++ thumb  : FastCanvasJNI <= FastCanvasJNI.cpp
    Compile++ thumb  : FastCanvasJNI <= Canvas.cpp
    StaticLibrary    : libstdc++.a
    SharedLibrary    : libFastCanvasJNI.so
    Install          : libFastCanvasJNI.so => libs/armeabi/libFastCanvasJNI.so
```
