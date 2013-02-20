/*global angular */
define(
[
  "ngModules",
  "bootstrap/bootstrap-button",
  "libs/angular-strap.min"
],
function(ngModules) {
  ngModules.add("angular-strap", ["$strap.directives"]);
});