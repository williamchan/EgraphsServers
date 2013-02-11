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
          get: function() {
            return $http.get(baseUrl);
          },

          url: baseUrl,

          egraph: function(form) {
            return $http.post(baseUrl + "/egraph", form);
          },

          discount: function(config) {
            if (config) {
              return $http.post(baseUrl + "/coupon", {"couponCode": config.code});
            } else {
              return $http.get(baseUrl + "/coupon");
            }
          },

          shippingAddress: function(form) {
            return $http.post(baseUrl + "/shipping-address", form);
          },

          buyer: function(form) {
            return $http.post(baseUrl + "/buyer", form);
          },

          recipient: function(postData) {
            if (postData) {
              return $http.post(baseUrl + "/recipient", postData.email);
            } else {
              return $http.get(baseUrl + "/recipient");
            }
          },

          payment: function(form) {
            return $http.post(baseUrl + "/payment", form);
          }
        };
      }
    };
  }]);
});