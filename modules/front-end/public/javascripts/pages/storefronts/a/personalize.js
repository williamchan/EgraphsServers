/* Scripting for the personalize page of the new base checkout design */
/*global angular */
define([
  "page",
  "libs/tooltip",
  "window",
  "services/logging",
  "module",
  "services/responsive-modal",
  "bootstrap/bootstrap-button"
],
function(page, tooltip, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var forEach = angular.forEach;

  return {
    go: function() {
      $(document).ready(function() {
        tooltip.apply();
      });
      log("All systems are go.");
    }
  };
});