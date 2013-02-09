/*
  Helps to configure a mock $httpBackend service for angular.
  See http://docs.angularjs.org/api/ngMockE2E.$httpBackend for more.
*/
/*global angular*/
define(
[
  "ngModules",
  "ngApp",
  "services/logging",
  "module",
  "libs/angular-mocks"
],
function(ngModules, ngApp, logging, module) {
  var log = logging.namespace(module.id);
  var e2eMockSupport = ngModules.add("E2ESupport", ["ngMockE2E"]);

  // Turns our $http calls back from form url encoded to json encoded for easier
  // mocking
  ngApp.run(function($http) {
    $http.defaults.headers.post['Content-Type'] = 'application/json';
    $http.defaults.transformRequest = [];
  });

  return {
    /** Sets the mock back-end behavior.

        configureHttpBackend - a function that accepts $httpBackend and configures it.
    */
    setBehavior: function(configureHttpBackend) {
      e2eMockSupport.run(function($httpBackend) {
        configureHttpBackend($httpBackend);
      });
    },

    /** Stubs a REST resource. argument is a configuration object with the following
        properties:

        path - a regex that matches the resource
        get - a function that accepts (query string data, requested url), and returns the stubbed resource
        post - a function that accepts (POST data, requested url), and returns the stubbed resource.
    */
    stubResource: function(obj) {
      var defaults = {path: /.*/, get: angular.noop, post: angular.noop};
      obj = angular.extend(defaults, obj);

      this.setBehavior(function($httpBackend) {
        $httpBackend.whenGET(obj.path).respond(function(method, url, data) {
          return obj.get(data, url);
        });

        $httpBackend.whenPOST(obj.path).respond(function(method, url, data) {
          return obj.post(data, url);
        });
      });
    }
  };
});