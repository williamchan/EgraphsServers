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

        startEvent: function(actionLabelAndSampleRate) {
          var action = actionLabelAndSampleRate[0];
          var label = actionLabelAndSampleRate[1];
          // Default to sample rate of 100
          var sampleRate = actionLabelAndSampleRate[3] || 100;
          
          return startEvent(function(durationMs) {
            var timingSpec = [action, durationMs, label, sampleRate];

            googleTrackTiming([categoryName].concat(timingSpec));
          });
        }
      };

      return self;
    },

    event: googleTrackEvent
  };
});