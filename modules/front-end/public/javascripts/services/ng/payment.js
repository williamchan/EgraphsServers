/* This file is in serious need of some dox */
/*global angular*/
define(
[
  "services/payment",
  "services/ng/validation-directive",
  "ngApp",
  "services/logging",
  "module",
  "services/ng/monitor-user-attention"
],
function(payment, validationDirective, ngApp, logging, module) {
  var log = logging.namespace(module.id);
  var forEach = angular.forEach;
  var noop = angular.noop;

  /** Name of all the form field directives */
  var directiveNames = {
    number: "creditCardNumber",
    cvc: "creditCardCvc",
    expiryMonth: "creditCardExpiryMonth",
    expiryYear: "creditCardExpiryYear"
  };

  var controlNameForErrorCode = function(errorCode) {
    return {
      "invalid_number": "cardNumber",
      "incorrect_number": "cardNumber",
      "card_declined": "cardNumber",
      "expired_card": "cardNumber",
      "processing_error": "cardNumber",
      "invalid_cvc": "securityCode",
      "incorrect_cvc": "securityCode",
      "invalid_expiry_month": "cardExpMonth",
      "invalid_expiry_year": "cardExpYear",

      // Response codes that only report the parameter in error
      "exp_year": "cardExpYear",
      "exp_month": "cardExpMonth",
      "number": "cardNumber",
      "cvc": "securityCode"
    }[errorCode];
  };

  ngApp.directive("stripeResource", ["$parse", "$http", function($parse, $http) {
    return {
      restrict: 'A',
      controller: ["$scope", function($scope) {
        var self = this;

        self.setForm = function(form) {
          self.form = form;
        };

        self.setErrors = function(stripeErrors) {
          forEach(stripeErrors, function(error) {
            var errorPrefix = "stripe_";
            var errorCode;
            var controlName;
            var control;
            if (error.indexOf(errorPrefix) === 0) {
              errorCode = error.replace(errorPrefix, "");
              controlName = controlNameForErrorCode(errorCode);
              control = self.form[controlName];
              if (control) {
                control.$setValidity("remote_" + errorCode, false);
              } else {
                self.form.$setValidity("remote_" + errorCode, false);
              }
            }
          });
        };
      }],
      require: ["stripeResource", "form", "remoteResource"],
      link: function(scope, element, attrs, requisites) {
        var tokenIdModel = attrs.stripeResource + ".id";
        var tokenDataModel = attrs.stripeResource + ".data";
        var stripeCtrl = requisites[0];
        var formCtrl = requisites[1];
        var remoteResourceCtrl = requisites[2];

        stripeCtrl.setForm(formCtrl);
        formCtrl.stripe = stripeCtrl;

        var getModel = function(modelStr) { return $parse(modelStr)(scope); };
        var setModel = function(modelStr, value) { return $parse(modelStr).assign(scope, value); };

        attrs.localResource = tokenIdModel;

        // When the stripe token ID changes due to successful form submission
        // we should also trigger a get of the remote token.
        scope.$watch(tokenIdModel, function(tokenId) {
          if(tokenId) {
            payment.getToken(tokenId, function(status, token) {
              scope.$apply(function() {
                if (status === 200) {
                  token.card.typeClass = function() {
                    return this.type === "Visa"? "visa":
                           this.type === "American Express"? "amex":
                           this.type === "MasterCard"? "mastercard":
                           this.type === "Discover"? "discover":
                           this.type === "JCB"? "jcb":
                           this.type;
                  };

                  setModel(tokenDataModel, token);
                } else {
                  setModel(tokenDataModel, undefined);
                  log("Error " + status + " retrieving token " + tokenId);
                }
              });
            });
          }
        });

        remoteResourceCtrl.setResourceBehavior({
          get: function(controls, onSuccess) {
            log("Intentionally neglecting GET on stripe resource form");
          },

          submit: function(controls, callbacks) {
            log("Submitting card info to stripe");
            var form = {
              number: controls["cardNumber"].$modelValue,
              cvc: controls["securityCode"].$modelValue,
              exp_month: parseInt(controls["cardExpMonth"].$modelValue, 10),
              exp_year: parseInt(controls["cardExpYear"].$modelValue, 10)
            };

            payment.createToken(form, function(status, response) {
              scope.$apply(function() {
                var errors = {};
                var data;
                if (status === 200) {
                  log("Stripe card submission accepted.");
                  data = response.id;
                  setModel(tokenIdModel, data);
                  callbacks.onSubmitSuccess(data);
                } else {
                  var errorCode = response.error.code? response.error.code: response.error.param;
                  var errorControlName = controlNameForErrorCode(errorCode);
                  errors[errorControlName] = [errorCode];

                  callbacks.onSubmitFail(data, errors);
                }

                callbacks.onSubmitEnd(data, errors);
              });
            });
          }
        });
      }
    };
  }]);
  
  // Helper for creating the input field directives that follow
  var ccFieldDirective = function(ngModule, name, validate) {
    validate = validate || function() { return true; };

    ngModule.directive(name, function() {
      return {
        'restrict': 'A',

        'require': ['ngModel', 'resourceProperty'],

        'link': function(scope, element, attrs, ctrls) {
          var inputCtrl = ctrls[0];

          // Create and add the parser where applicable
          inputCtrl.$parsers.push(validationDirective.parser(name, inputCtrl, validate));
        }
      };
    });
  };

  // Provides support for <input credit-card-number>
  var creditCardNumberDirective = function(ngModule) {
    ccFieldDirective(ngModule, directiveNames.number, function(viewValue) {
      if (viewValue) {
        return payment.validateCardNumber(viewValue);
      } else {
        return true;
      }
    });
  };

  // Provides support for <input credit-card-cvc>
  var creditCardCvcDirective = function(ngModule) {
    ccFieldDirective(ngModule, directiveNames.cvc, function(viewValue) {
      if (viewValue) {
        return payment.validateCVC(viewValue);
      } else {
        return true;
      }
    });
  };

  // Provides support for <input credit-card-expiry-month> and <input credit-card-expiry-year>
  var creditCardExpiryDirectives = function(ngModule) {
    ccFieldDirective(ngModule, directiveNames.expiryMonth);
    ccFieldDirective(ngModule, directiveNames.expiryYear);
  };

  var applyDirectives = function(ngModule) {
    creditCardNumberDirective(ngModule);
    creditCardCvcDirective(ngModule);
    creditCardExpiryDirectives(ngModule);

    return ngModule;
  };

  return applyDirectives(ngApp);
});
