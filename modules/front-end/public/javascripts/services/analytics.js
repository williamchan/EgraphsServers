define(
[
  "window",
  "services/logging",
  "module"
],
function(window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var google = window._gaq;

  var googleEvent = function(args) {
    var gaqArguments = ["_trackEvent"].concat(args);
    log("Google Event: " + args);
    google.push(gaqArguments);
  };

  var startEvent = function(onComplete) {
    var start = new Date().getTime();
    return {
      track: function() {
        var end = new Date().getTime();
        onComplete(end - start);
      }
    };
  };

  return {
    eventCategory: function(categoryName) {
      var self = {
        track: function(actionAndLabel) {
          googleEvent([categoryName].concat(actionAndLabel));
        },

        startEvent: function(actionAndLabel) {
          return startEvent(function(durationMs) {
            self.track(actionAndLabel.concat(durationMs));
          });
        }
      };

      return self;
    },

    event: googleEvent
  };
});