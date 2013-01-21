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
define(["page", "window"], function(page, window) {
  var config = page.logging || {level:"all"};
  var disabled = config.level !== "all";
  var console = window.console;
  var noop = angular.noop;

  var log = disabled? noop: function(msg) {
    if (console) console.log(msg);
  };

  return {
    /**
     * A generic logging function. Writes directly to window.console if it exists, otherwise noop.
     * 
     * @param message a message to log, of any type.
     */
    log: log,

    /** 
     * Returns a namespaced logging function. All logs written using the function returned
     * will be prefaced with the namespace.
     *
     * See module description for more information.
     *
     * @param ns the namespace to apply to the function (usually the active module id)
     **/
    namespace: function(ns) {
      var nsString = "[" + ns.toString() + "]";
      return disabled? noop: function(message) {
        if (typeof message === "object") {
          log(nsString + " (the following object was logged ---v)");
          log(message);
        } else {
          log(nsString + " " + message);
        }
      };
    }
  };
});