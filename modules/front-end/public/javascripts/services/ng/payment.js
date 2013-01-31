/**
 * Angular directives for creating forms for submitting credit card information
 * to our payment partners. Complements the existing angular forms API. To use, call
 * this module's applyDirectives method upon your angular module.
 *
 * See the form named "pay" in gift_certificate_checkout.scala.html for example usage.
 * The basic structure is:
 *
 *   <!-- Create a form with the directive, and give it a name. -->
 *   <form credit-card-form name="ccForm">
 *     <!-- Create necessary form inputs for card: number, cvc, expiry month, expiry year
 *          The form inputs will automatically use payment.js's client-side validation methods.
 *          You can name them whatever you want, but they do need a name. -->
 *     <input credit-card-number name="cardNumber" type="text" />
 *
 *     <!-- Hook into errors using angular's nice declarative syntax-->
 *     <div class="errors" ng-show="pay.cardNumber.$invalid" >
 *       <div ng-show="pay.cardNumber.$error.remote_payment_invalid_number || pay.cardNumber.$error.invalid_number" >
 *         Card number is invalid yo
 *       </div>
 *       <div ng-show="pay.cardNumber.$error.remote_payment_incorrect_number" >
 *         Card number is incorrect yo...whatever that means.
 *       </div>
 *     </div>
 *
 *     <input credit-card-cvc name="cardCvc" type="text" />
 *     <input credit-card-expiry-month name="cardExpMonth" type="text" />
 *     <input credit-card-expiry-year name="cardExpYear" type="text" />
 *     <!-- Submission on ng-click. This example presupposes your controller defined a handler named
 *            $scope.onCardInfoValidated, which takes the tokenId as its only parameter. That's where
 *            all your work happens, and it'll only happen if the CC info was valid!
 *
 *            If the CC info was invalid, then the error boxes we specified above will appear up in
 *            your form -->
 *     <button type="button" ng-click="creditCardFormSubmit(onCardInfoValidated)" >
 *   </form>
 */
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

  ngApp.directive("stripeResource", ["$parse", "$http", function($parse, $http) {
    return {
      restrict: 'A',
      scope: true,
      require: ["remoteResource"],
      link: function(scope, element, attrs, requisites) {
        var tokenIdModel = attrs.stripeResource + ".id";
        var tokenDataModel = attrs.stripeResource + ".data";
        var remoteResourceCtrl = requisites[0];
        var getModel = function(modelStr) { return $parse(modelStr)(scope.$parent); };
        var setModel = function(modelStr, value) { return $parse(modelStr).assign(scope.$parent, value); };

        attrs.localResource = tokenIdModel;

        // When the stripe token ID changes due to successful form submission
        // we should also trigger a get of the remote token.
        scope.$parent.$watch(attrs.localResource, function(tokenId) {
          if(tokenId) {
            payment.getToken(tokenId, function(status, token) {
              scope.$parent.$apply(function() {
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
            "invalid_expiry_year": "cardExpYear"
          }[errorCode];
        };

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
              scope.$parent.$apply(function() {
                var errors;
                var data;
                if (status === 200) {
                  log("Stripe card submission accepted.");
                  data = response.id;
                  setModel(tokenIdModel, data);
                  callbacks.onSubmitSuccess(data);
                } else {
                  var errorCode = response.error.code;
                  var errorControlName = controlNameForErrorCode(errorCode);
                  errors = [{field:errorControlName, cause:errorCode}];

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
