/**
  Angular service for subscribing emails to our mailchimp mailing list
  Example usage (with requirejs boilerplate):

  // Define your page module
  define(["services/ng/mail-services"], function() {
    return {
      ngControllers: {
        MailCtrl: ["$scope", "$subscribe", function($scope, $subscribe) {
          $scope.subscribe = function() {
            subscribeService($scope.email);
          }
        }]
      };
    }
  })

  // A basic form
  <form ng-controller="MailCtrl" ng-submit="subscribe()">
    <input type="email" ng-model="email">
    <button type="submit"></button>
  </form>
**/
/*global angular, mixpanel*/
define(["page", "services/logging", "module", "ngModules"],
  function(page, logging, requireModule, ngModules) {
    var log = logging.namespace(requireModule.id);
    var mail = page.mail;
    var authToken = page.authenticityToken;
    var subscribeFactory = function($http) {
      return function(email, successCallback, errorCallback) {
        log(email);
        $http({
          method: 'POST',
          url: mail.url,
          data: {"email" : email, "authenticityToken" : authToken}
        }).success( function(data) {
          log("Subscribed!");
          mixpanel.track('Subscribed to newsletter');
          (successCallback || angular.noop)(data);
        }).error( function(data) {
          (errorCallback || angular.noop)(data);
        });
      };
    };

    // Angular module for subscribing to the newsletter
    ngModules.add('MailServices').factory('$subscribe', ["$http", subscribeFactory]);
    
    return subscribeFactory;
  }
);