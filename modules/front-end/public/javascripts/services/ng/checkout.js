/**
  Services for accessing the checkout portion of the Egraphs API
*/
/*global angular*/
define(
[
  "page",
  "ngApp",
  "services/logging",
  "module",
  "services/ng/http-config",
  "services/ng/resource-form"
],
function(page, ngApp, logging, module) {
  var log = logging.namespace(module.id);
  var sessionId = page.sessionId;
  var apiRoot = page.apiRoot;
  var authToken = page.authenticityToken;
  var extend = angular.extend;
  var noop = angular.noop;

  ngApp.factory("$checkout", ["$http", function($http) {
    return {
      forStorefront: function(celebId) {
        var baseUrl = apiRoot + "/sessions/" + sessionId + "/checkouts/" + celebId;

        return {
          url: baseUrl,

          get: function() {
            return $http.get(baseUrl);
          },

          transact: function() {
            return $http.post(baseUrl);
          }
        };
      }
    };
  }]);
});