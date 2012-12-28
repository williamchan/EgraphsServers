define([], function() {
  var log = function(msg) {
    if (window.console) window.console.log(msg);
  };

  return {
    log: log,
    namespace: function(ns) {
      var nsString = "[" + ns.toString() + "]";
      return function(message) {
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