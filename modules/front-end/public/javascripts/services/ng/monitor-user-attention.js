/**
  Configures an angular directive that monitors user "attention". Here, attention is defined
  by having focused then un-focused on an element. The directive sets some configuration onto
  the NgModel controller under the "userAttention" property. It also sets the class
  ng-user-has-attended on the input itself.

  Access whether an NgModel controller has been attended to by the user by reading
  controller.userAttention.attended

  The directive is "monitor-user-attention"
*/
/*global angular*/
define(
[
  "ngApp",
  "services/analytics",
  "window",
  "services/logging",
  "module"
],
function(ngApp, analytics, window, logging, module) {
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
        var analyticsCategory = scope.$parent.analyticsCategory?
          scope.$parent.analyticsCategory:
          window.location.href;
        var events = analytics.eventCategory(analyticsCategory);

        inputControl.userAttention = userAttention;

        element.focus(function() {
          var durationEvent = events.startEvent(["Control focus removed", attrs.ngModel]);
          var onBlur = function() {
            scope.$apply(function() {
              userAttention.setAttended(true);
            });

            element.unbind('blur', onBlur);
            durationEvent.track();
          };

          events.track(["Control focused", attrs.ngModel]);

          element.bind('blur', onBlur);
        });

        userAttention.listeners.push(function() {
          element.addClass("ng-user-has-attended");
        });
      }
    };
  }]);
});