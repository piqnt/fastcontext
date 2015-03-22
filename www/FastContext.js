(function(cordova, require) {

  var _exec = cordova && cordova.exec || require && require('cordova/exec');

  function exec(done, fail, action, args) {
    if (done && typeof done !== 'function') {
      throw new Error('Invalid "done" callback type: ' + typeof done);
    }
    if (fail && typeof fail !== 'function') {
      throw new Error('Invalid "fail" callback type: ' + typeof fail);
    }

    if (_exec) {
      _exec(done, fail, 'FastContext', action, args || []);
    } else if (typeof fail === 'function') {
      fail();
    }
  }

  var isAvailable = false;

  exec(function(availabe) {
    isAvailable = availabe;
    console.log('FastContext isAvailable: ' + isAvailable);
  }, function() {
    isAvailable = false;
    console.log('FastContext isAvailable: ' + isAvailable);
  }, 'isAvailable');

  HTMLCanvasElement.prototype.getContext = (function(getContext) {
    return function(type, opts) {
      var context;
      if (opts && opts.fastcontext && isAvailable) {
        context = new FastContext();
        context.isFast = true;
      } else {
        context = getContext.apply(this, arguments)
        context.isFast = false;
      }
      console.log('FastContext getContext: ' + type + ', ' + context.isFast);
      return context;
    }
  })(HTMLCanvasElement.prototype.getContext);

  function FastContext() {
    this._commands = ""; // TODO: use binary array instead of string
    this.globalAlpha = this._globalAlpha = 1.0;

    var self = this, timeout = null;
    this.flush = function(defer) {
      if (defer) {
        if (timeout === null) {
          timeout = setTimeout(self.flush, 1);
        }
      } else {
        if (timeout !== null) {
          clearTimeout(timeout);
          timeout = null;
        }
        exec(null, null, 'render', [ self._commands ]);
      }
    }
  }

  FastContext.prototype.clear = function() {
    this.flush(true);
    this._commands = "";
  };

  FastContext.prototype.setTransform = function(a, b, c, d, tx, ty) {
    this.flush(true);
    a = num(a), b = num(b), c = num(c), d = num(d), tx = num(tx), ty = num(ty);
    this._commands += "t" + a + "," + b + "," + c + "," + d + "," + tx + ","
        + ty + ";";
  };

  FastContext.prototype.transform = function(a, b, c, d, tx, ty) {
    this.flush(true);
    a = num(a), b = num(b), c = num(c), d = num(d), tx = num(tx), ty = num(ty);
    this._commands += "f" + a + "," + b + "," + c + "," + d + "," + tx + ","
        + ty + ";";
  };

  FastContext.prototype.resetTransform = function() {
    this.flush(true);
    this._commands += "m;";
  };

  FastContext.prototype.scale = function(x, y) {
    this.flush(true);
    x = num(x), y = num(y);
    this._commands += "k" + x + "," + y + ";";
  };

  FastContext.prototype.rotate = function(a) {
    this.flush(true);
    a = num(a);
    this._commands += "r" + a + ";";
  };

  FastContext.prototype.translate = function(tx, ty) {
    this.flush(true);
    tx = num(tx), ty = num(ty);
    this._commands += "l" + tx + "," + ty + ";";
  };

  FastContext.prototype.save = function() {
    this.flush(true);
    this._commands += "v;";
  };

  FastContext.prototype.restore = function() {
    this.flush(true);
    this._commands += "e;";
  };

  FastContext.prototype.drawImage = function(image, sx, sy, sw, sh, dx, dy, dw,
      dh) {
    this.flush(true);

    if (this._globalAlpha !== this.globalAlpha) {
      this._globalAlpha = this.globalAlpha;
      this._commands += "a" + num(this.globalAlpha) + ";";
    }

    if (typeof dx !== 'undefined') {
      // all arguments, source and destination
      this._commands += "d" + image._id + "," + sx + "," + sy + "," + sw + ","
          + sh + "," + dx + "," + dy + "," + dw + "," + dh + ";";

    } else if (typeof sw !== 'undefined') {
      // drawImage(image, dx, dy, dw, dh); position and size (s becomes d)
      this._commands += "d" + image._id + ",0,0," + image.width + ","
          + image.height + "," + sx + "," + sy + "," + sw + "," + sh + ";";

    } else {
      // drawImage(image, dx, dy); position only (s becomes d)
      this._commands += "d" + image._id + ",0,0," + image.width + ","
          + image.height + "," + sx + "," + sy + "," + image.width + ","
          + image.height + ";";
    }
  };

  FastContext.prototype.capture = function(x, y, w, h, fileName, done, fail) {
    exec(done, fail, 'capture', [ x, y, w, h, fileName ]);
  };

  FastContext.prototype.setBackgroundColor = function(color) {
    if (typeof color === 'number') {
      color = color.toString(16);
      while (color.length < 6) {
        color = '0' + color;
      }
    } else if (typeof color === 'string'
        && (color = color.match(/(#|0x)?([0-9a-f]{6}|[0-9a-f]{3})/i))) {
      color = color[2]
      if (color.length === 3) {
        color = color.charAt(0) + color.charAt(0) + color.charAt(1)
            + color.charAt(1) + color.charAt(2) + color.charAt(2);
      }
    } else {
      return false;
    }
    exec(null, null, 'setBackgroundColor', [ color ]);
    return true;
  };

  FastContext.prototype.preload = function(src, done, fail) {
    return new FastContextImage(src, done, fail);
  };

  // TODO remove this!
  FastContext.createImage = function(src, done, fail) {
    if (isAvailable) {
      return new FastContextImage(src, done, fail);
    } else {
      var image = new Image();
      image.onload = done;
      image.onerror = fail;
      image.src = src;
      return image;
    }
  };

  var ID = 0;

  function FastContextImage(src, done, fail) {

    this.width = 0;
    this.height = 0;
    this.id = this._id = ++ID;

    var self = this;
    exec(function(metrics) {
      self.width = Math.floor(metrics[0]);
      self.height = Math.floor(metrics[1]);
      done && done();

    }, function(err) {
      fail && fail(err);

    }, 'loadTexture', [ src, this._id ]);
  }

  this.FastContext = window.FastContext = FastContext;

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = FastContext;
  }

  function num(x) {
    return (x * 1000000 | 0) / 1000000;
  }

}).call(this, typeof cordova !== "undefined" ? cordova : null,
    typeof require === 'function' ? require : null);