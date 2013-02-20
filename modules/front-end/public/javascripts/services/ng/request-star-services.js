/**
  Angular service for requesting a star
  (See mail-services.js for sample usage)
**/

/*global angular, mixpanel*/
define(["page", "ngApp", "services/logging", "module"],
  function(page, ngApp, logging, requireModule) {
    var log = logging.namespace(requireModule.id);
    var request = page.request;
    var authToken = page.authenticityToken;
    var requestStarFactory = function($http) {
      return function(starName, email, successCallback, errorCallback) {
        log(email);
        $http({
          method: 'POST',
          url: request.url,
          data: {"starName" : starName, "email" : email, "authenticityToken" : authToken}
        }).success( function(data) {
          log("Star requested!");
          mixpanel.track('Star requested');
          (successCallback || angular.noop)(data);
        }).error( function(data) {
          (errorCallback || angular.noop)(data);
        });
      };
    };

    // Angular module for requesting a star
    ngApp.factory('$requestStar', ["$http", requestStarFactory]);
    
    return requestStarFactory;
  }
);