> FastContext is a based on [FastCanvas](https://github.com/phonegap/phonegap-plugin-fast-canvas) by [Jeff Mott](https://github.com/jmott), but instead of creating a Canvas element it extends Canvas prototype and transparently returns a FastContext if available and requested in second parameter of `Canvas#getContex()`.

# FastContext

FastContext is a Cordova/PhoneGap plugin for Android which replaces HTML5 2D Context with a very fast native GL rendering surface.

While FastContext attempts to look and behave very similar to the standard 2D Context, it only supports a subset of standard API, focusing on what benefits most from hardware acceleration. See [Usage](#usage) and [API](#api) sections for more information.

Unlike standard 2D Context, FastContext encompasses your entire screen and can not be integrated with other elements in the DOM. It lives outside of the DOM in a separate rendering surface and covers HTML content.

### Installation

```
cordova plugin add https://github.com/shakiba/fastcontext.git
```

### Usage

Usage is very similar to standard 2D Context with few exception:

* A second parameter with `fastcontext` attribute set to true is passed to `canvas.getContext`, it will return a `FastContext` if available.
* Images are created and loaded with `context.preload()`, only local image files are supported.
* `context.clear()` should be called instead of `context.clearRect()`, it clears entire Canvas.

```javascript
var context = canvas.getContext('2d', { fastcontext : true });

var image, src = "graphics/image.jpg";
if (context.isFast) {
  image = context.preload(src, render, console.log);
} else {
  image = new Image();
  image.src = "graphics/image.jpg";
  image.onload = render;
  image.onerror = console.log;
}

function render() {
  if (context.isFast) {
    context.clear();
  } else {
    context.clearRect(0, 0, width, height);
  }
  context.translate(100, 100);
  context.rotate(Math.PI);
  context.drawImage(image, 0, 0);
}
```

In addition to the code changes above, because FastContext applications are fullscreen, your HTML should also include the following meta tag to be assured that window metrics are reported accurately and consistently:

```html
<meta name="viewport" content="user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, width=device-width, height=device-height" />
```

### API

FastContext API consists a subset of the standard [CanvasRenderingContext2D API](http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html#canvasrenderingcontext2d) and few additional members.

| Member | Notes |
| ------ | ----- |
| context.isFast | `true` if it is a FastContext. |
| context.preload(image, onsuccess, onerror) | Preload images in native context. |
| context.clear() | Should be used instead of clearRect(), it clears entire Canvas. |
| context.flush() | Flushes queued commands to native backend, called automatically at the end of each frame. |
| context.capture(x, y, w, h, filename, onsuccess, onerror) | Saves the current state of the Context as an image. |
| context.setBackgroundColor(color) | Set background color for context, accepts hex format. |
| context.drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh) | Supported |
| context.globalAlpha | Supported |
| context.resetTransform() | Supported |
| context.restore() | Supported |
| context.rotate(angle) | Supported |
| context.save() | Supported |
| context.scale(x, y) | Supported |
| context.setTransform(a, b, c, d, e, f) | Supported |
| context.transform(a, b, c, d, e, f) | Supported |
| context.translate(x, y) | Supported |
| context.clearRect() | Not supported, use context.clear() instead. |

### Best practice

FastContext creates an OpenGL surface that sits on top of the browser and will cover any HTML output, therefore you should generally avoid HTML rendering as it will only consume performance.

For best performance, minimize the number of draw calls per frame in the GL layer, at the JavaScript level it means: First, use sprite sheets and use as few textures as possible. Second, preload textures if possible and avoid swapping them in and out. Finally, try to batch drawImage calls that use the same texture. It is vastly more efficient to make ten drawImage calls in a row using one texture, and then make ten more using a second texture, than to switch back and forth twenty times.

### How it works?

The renderer itself is OpenGL ES 2.0 command streams and the code is written in C++. The advantage of C++ is both portability and control of memory management.

Your JS code runs in the browser thread, while most of the work FastContext does is in the Android UI thread. A tight stream of rendering commands is sent from JS to UI thread.

### TODO

* Send absolute (resolved) images path to load.
* Make it possible to show/hide GLSurface.
