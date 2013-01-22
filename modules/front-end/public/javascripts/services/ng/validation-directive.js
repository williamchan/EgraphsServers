/**
 * Makes custom angular validation directives easier to implement. To use,
 * import "services/ng/validation-directive".
 */
define([],
function() {
  return {
    /**
     * Adds a validation directive to the provided angular module.
     * If a tag decorated with the directive name fails to pass the
     * provided validate function, it will be classed with "directiveName-invalid".
     *
     * Example:
     * validationDirective.applyToModule(myModule, "cat", function(toValidate) {
     *   return toValidate === "a cat";
     * });
     */
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

    /**
     * Returns a parser for use in directive validation.
     * Use in the 'link' function of a new directive.
     *
     * Example:
     * ngModel.$parsers.push(validationDirective.parser("cat", ngModel, function(tovalidate) {
     *   return tovalidate === "a cat";
     * }));
     */
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
