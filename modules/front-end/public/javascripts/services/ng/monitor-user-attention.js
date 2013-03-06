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
      controller: ["$scope", function($scope) {
        var self = this;

        this.listeners = [];
        this.attended = false;
        
        this.setElement = function(element) {
          self.element = element;
        };

        this.setAttended = function(hasAttended) {
          var element = self.element;
          var isFirstAttention = !this.$attended && hasAttended;

          self.attended = hasAttended;

          if (isFirstAttention) {
            forEach(self.listeners, function(listener) {
              listener();
            });
          }
        };
      }],

      require: ['ngModel', 'monitorUserAttention'],

      link: function(scope, element, attrs, requisites) {
        var inputControl = requisites[0];
        var userAttention = requisites[1];
        var analyticsCategory = scope.analyticsCategory?
          scope.analyticsCategory:
          window.location.href;
        var events = analytics.eventCategory(analyticsCategory);

        userAttention.setElement(element);

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