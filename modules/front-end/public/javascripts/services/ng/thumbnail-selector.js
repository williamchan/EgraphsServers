/**
 * Code used to make an angular input out of the thumbnail selector on the personalize
 * page.
 */
/*global angular*/
define(
[
  "ngApp",
  "services/logging",
  "module"
],
function(ngApp, logging, module) {
  var log = logging.namespace(module.id);
  var extend = angular.extend;
  var forEach = angular.forEach;

  /**
   * Apply this directive to the div you wish to act as the high-level control for the
   * thumbnail selector. Requires an ngModel as with any other angular control.
   */
  ngApp.directive('thumbnailSelector', function() {
    return {
      controller: ["$scope", "$parse", "$attrs", function($scope, $parse, $attrs) {
        var self = this;
        self.collection = $parse($attrs.collection)($scope);
        var thumbsById = {};

        forEach(self.collection, function(item) {
          if (item.selected) {
            self.selectedThumb = item;
          }
        });

        self.addThumb = function(thumbController) {
          thumbsById[thumbController.id] = thumbController;
          // view -> model
          thumbController.element.bind('click', function() {
            $scope.$apply(function() {
              self.ngModel.$setViewValue(thumbController.id);
            });
          });
        };

        self.setNgModel = function(ngModel) {
          self.ngModel = ngModel;
          self.ngModel.$setViewValue(self.selectedThumb.id);

          // model -> view
          $scope.$watch($attrs.ngModel, function(newId, oldId) {
            var newThumb = thumbsById[newId];
            var oldThumb = thumbsById[oldId];

            if (oldThumb) oldThumb.select(false);
            if (newThumb) newThumb.select(true);
          });
        };
      }],

      require: ['ngModel', "thumbnailSelector"],
      link: function(scope, elem, attrs, controllers) {
        var ngModel = controllers[0];
        var thumbnailSelector = controllers[1];

        thumbnailSelector.setNgModel(ngModel);
      }
    };
  })

  /**
   * Add this directive to each thumb that you wish to be clickable and selectable.
   * It should have a "value" attribute that represents the value of the model when
   * that thumb is selected.
   */
  .directive('thumb', function() {
    return {
      controller: ["$scope", "$interpolate", "$attrs", function($scope, $interpolate, $attrs) {
        var self = this;
        self.id = $interpolate($attrs.value)($scope);

        self.setElement = function(element) {
          self.element = element;
        };

        self.select = function(doSelect) {
          if (doSelect) {
            self.element.addClass("selected");
          } else {
            self.element.removeClass("selected");
          }
        };
      }],
      require: ['^thumbnailSelector', "thumb"],
      link: function(scope, elem, attrs, controllers) {
        var selectorCntrl = controllers[0];
        var thisCntrl = controllers[1];

        thisCntrl.setElement(elem);
        selectorCntrl.addThumb(thisCntrl);
      }
    };
  });
});