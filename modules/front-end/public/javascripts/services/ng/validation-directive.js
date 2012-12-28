/** Function to makes custom validation directives easier to implement */
define([],
function() {
  return {
    applyToModule: function(ngModule, directiveName, validate) {
      ngModule.directive(directiveName, function() {
        return {
          require: 'ngModel',
          link: function(scope, element, attrs, ctrl) {
            ctrl.$parsers.push(function(viewValue) {
              if (validate(viewValue)) {
                ctrl.$setValidity(directiveName, true);
                return viewValue;
              } else {
                ctrl.$setValidity(directiveName, false);
                return undefined;
              }
            });
          }
        };
      });
    },

    parser: function(directiveName, ctrl, validate) {
      return function(viewValue) {
        if (validate(viewValue)) {
          ctrl.$setValidity(directiveName, true);
          return viewValue;
        } else {
          ctrl.$setValidity(directiveName, false);
          return undefined;
        }
      };
    }
  };
});
