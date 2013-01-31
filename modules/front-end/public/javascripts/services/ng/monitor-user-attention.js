/*global angular*/
define(
[
  "ngApp",
  "services/logging",
  "module"
],
function(ngApp, logging, module) {
  var log = logging.namespace(module.id);
  var forEach = angular.forEach;

  ngApp.directive("monitorUserAttention", [function() {
    return {
      restrict: 'A',
      scope: true,
      controller: ["$scope", function($scope) {
        this.listeners = [];
        this.attended = false;
        this.setAttended = function(hasAttended) {
          var element = $scope.element;
          var isFirstAttention = !this.$attended && hasAttended;

          this.attended = hasAttended;

          if (isFirstAttention) {
            forEach(this.listeners, function(listener) {
              listener();
            });
          }
        };
      }],

      require: ['ngModel', 'monitorUserAttention'],

      link: function(scope, element, attrs, requisites) {
        var inputControl = requisites[0];
        var userAttention = requisites[1];

        inputControl.userAttention = userAttention;

        element.bind('blur', function() {
          scope.$apply(function() {
            userAttention.setAttended(true);
          });
        });

        userAttention.listeners.push(function() {
          element.addClass("ng-user-has-attended");
        });
      }
    };
  }]);
});