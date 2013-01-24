/**
  Angular service for subscribing emails to our mailchimp mailing list
  Example usage:
  // Define a stub controller
  var mailCtrl = function($scope, subscribeService) {
    $scope.subscribe = function() {
      subscribeService($scope.email);
    };
  }
  // Bootstrap the angular dependencies
  angular.element(document).ready(function() {
    window.MailCtrl = mailCtrl;
    window.MailCtrl.$inject = ['$scope', 'subscribe'];
    angular.bootstrap(document, ['MailServices']);
  });

  // A basic form
  <form ng-controller="MailCtrl" ng-submit="subscribe()">
    <input type="email" ng-model="email">
    <button type="submit"></button>
  </form>

  // This service depends on an authToken here Egraphs.page.authenticityToken.
**/

define(["page", "window", "services/logging", "module"],
  function(page, window, logging, requireModule) {
    var log = logging.namespace(requireModule.id);
    var mail = page.mail;
    var authToken = page.authenticityToken;

    // Angular module for subcscribing to the newsletter
    angular.module('MailServices', []).factory('subscribe', ['$http', function($http) {
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
    }]);
  }
);