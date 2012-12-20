/** Provides angular bindings to payment services */
define(["services/payment"],
function(payment) {
  return {
    /** Apply to an ng.module to make payment directives available to associated page */
    applyDirectives: function(ngModule) {
      // credit-card-number validation
      var ccDirectiveName = "creditCardNumber";
      ngModule.directive(ccDirectiveName, function() {
        return {
          require: 'ngModel',
          link: function(scope, element, attrs, ctrl) {
            ctrl.$parsers.push(function(viewValue) {
              if (payment.validateCardNumber(viewValue)) {
                ctrl.$setValidity(ccDirectiveName, true);
                return viewValue;
              } else {
                ctrl.$setValidity(ccDirectiveName, false);
                return undefined;
              }
            });
          }
        };
      });
    }
  };
});
