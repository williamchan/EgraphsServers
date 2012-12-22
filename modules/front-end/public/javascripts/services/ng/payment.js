/** Provides angular bindings to payment services */
define(["services/payment", "services/ng/validation-directive"],
function(payment, validationDirective) {
  var creditCardNumberDirective = function(ngModule) {
    validationDirective(ngModule, "creditCardNumber", function(viewValue) {
      if (viewValue) {
        return payment.validateCardNumber(viewValue);
      } else {
        return true;
      }
    });
  };

  var creditCardCvcDirective = function(ngModule) {
    validationDirective(ngModule, "creditCardCvc", function(viewValue) {
      if (viewValue) {
        return payment.validateCVC(viewValue);
      } else {
        return true;
      }
    });
  };

  return {
    /** Apply to an ng.module to make payment directives available to associated page */
    applyDirectives: function(ngModule) {
      creditCardNumberDirective(ngModule);
      creditCardCvcDirective(ngModule);
    }
  };
});
