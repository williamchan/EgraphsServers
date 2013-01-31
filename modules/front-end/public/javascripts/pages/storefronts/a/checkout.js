/* Scripting for the checkout page of the new base checkout design */
/*global angular */
define([
  "page",
  "window",
  "services/logging",
  "module",
  "services/responsive-modal",
  "bootstrap/bootstrap-button"
],
function(page, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var forEach = angular.forEach;

  return {
    go: function() {

      log("All systems are go.");
    }
  };
});