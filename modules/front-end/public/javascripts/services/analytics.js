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

  var googleTrackSocialEvent = function(args) {
    var gaqArguments = ["_trackSocial"].concat(args);
    log("Google Social Event: " + args);
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
      var trackInCategory = function(actionAndLabel) {
        googleTrackEvent([categoryName].concat(actionAndLabel));
      };

      var socialEvent = function(network, action) {
        var networkAndAction = [network, action];
        return {
          track: function(targetUrl) {
            // filter falsy values (e.g. undefined if called with no args)
            var targetArgArray = [targetUrl].filter(Boolean); 
            
            // not sure whether social events are as useful as normal events, so let's track both
            trackInCategory(networkAndAction);
            googleTrackSocialEvent(networkAndAction.concat(targetUrlArray));
          }
        }
      };

      var self = {
        track: function(actionAndLabel) {
          trackInCategory(actionAndLabel);
        },

        // tracking for social events (e.g. widget interactions); organized by network, then action,
        // and then finally a track method
        social: {
          fb: {
            like:   socialEvent('Facebook', 'Like'),
            unlike: socialEvent('Facebook', 'Unlike'),
            share:  socialEvent('Facebook', 'Share')
          },

          twitter: {
            tweet: {
              // https://developers.google.com/analytics/devguides/collection/gajs/gaTrackingSocial#twitter
              track: function(intent_event) {
                if (intent_event) {
                  var opt_pagePath;
                  if (intent_event.target && intent_event.target.nodeName == 'IFRAME') {
                    opt_target = extractParamFromUri(intent_event.target.src, 'url');
                  }
                  socialEvent('Twitter', 'Tweet').track(opt_pagePath);
                }
              }
            }
          },

          pinterest: {
            pin: socialEvent(['Pinterest', 'Pinned'])
          }
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