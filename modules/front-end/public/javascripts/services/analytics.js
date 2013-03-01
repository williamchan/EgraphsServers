define(
[
  "window",
  "services/logging",
  "module"
],
function(window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var google = window._gaq;

  var googleTrackEvent = function(args) {
    var gaqArguments = ["_trackEvent"].concat(args);
    log("Google Event: " + args);
    google.push(gaqArguments);
  };

  var googleTrackTiming = function(args) {
    var gaqArguments = ["_trackTiming"].concat(args);
    log("Google Timing Event: " + args);
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
          googleTrackEvent([categoryName].concat(actionAndLabel));
        },

        startEvent: function(actionAndLabel) {
          var action = actionAndLabel[0];
          var label = actionAndLabel[1];
          
          return startEvent(function(durationMs) {
            var timingSpec = [action, durationMs];
            if (label) timingSpec.push(label);

            googleTrackTiming([categoryName].concat(timingSpec));
          });
        }
      };

      return self;
    },

    event: googleTrackEvent
  };
});