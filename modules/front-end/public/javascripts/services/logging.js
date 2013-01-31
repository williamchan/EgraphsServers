/**
 * Logging implementation for usage in production and testing.
 * 
 * Can be configured by the currently serving page's headJs:
 *   angular.extend(Egraphs.page, {
 *      logging: {
 *        level: "all"; // Optional. "all" (default), "off".
 *      };
 *   });
 *
 * Best used when coupled with the automatic "module" import of require.js, e.g. (pages/my_page.js):
 *  define(["services/logging", "module"], function(logging, module) {
 *    var log = logging.namespace(module.id);
 *
 *    log("Hello") // [pages/my_page] Hello
 *    log({ "name" : "Jimmy Wiggles" }) // [pages/my_page] (the following object was logged --v)
 *                                      //   [[In Chrome, a nicely formatted object would be here]]
 *  });
 **/
/*global angular*/
define(["page", "window", "module"], function(page, window, module) {
  var config = page.logging || {level:"all"};
  var disabled = config.level !== "all";
  var console = window.console;
  var noop = angular.noop;

  var log = disabled? noop: function(msg) {
    if (console) console.log(msg);
  };

  var lineNumber = function() {
    try {
      var error = (new Error());
      var stack = error.stack;
      var frames = stack.split("\n");
      var numFrames = frames.length;
      var lineNumberRegex = /:([0-9]+):[0-9+]/;
      var i;
      var loggingFrame;
      var lineNumber;

      for (i = 1; i < numFrames && loggingFrame === undefined; i++) {
        var frame = frames[i];
        if (frame.indexOf(module.id) === -1) {
          loggingFrame = frame;
          break;
        }
      }

      var match = lineNumberRegex.exec(loggingFrame);
      if (match !== null && match.length > 1) {
        lineNumber = match[1];
      }
      return lineNumber;

    } catch(err) {
      return undefined;
    }
  };

  var logModule = function(message) {
    var source = lineNumber();
  };

  logModule.namespace = function(ns) {
    var nsString = ns.toString();

    return disabled? noop: function(message) {
      var line = lineNumber();
      var nsAndLine = line? (nsString + ":" + line): nsString;
      var nsPrefix = "[" + nsAndLine + "]";

      if (typeof message === "object") {
        log(nsPrefix + " (the following object was logged ---v)");
        log(message);
      } else {
        log(nsPrefix + " " + message);
      }
    };
  };

  return logModule;
});