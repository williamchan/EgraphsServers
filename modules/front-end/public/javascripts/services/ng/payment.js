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
define(["services/payment", "services/ng/validation-directive", "services/logging", "module"],
function(payment, validationDirective, logging, module) {
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

  // Directive that supplies support for <form credit-card-form name="cc">...</form>
  var creditCardFormDirective = function(ngModule) {
    ngModule.directive('creditCardForm', function($rootScope) {

      return {
        'restrict': 'A',

        'scope': true,

        'controller': function($scope, $element, $attrs) {
          var self = this;

          angular.extend(self, {
            formComponents: {},
            paymentControls: {},

            registerFormComponent: function (paymentDirectiveName, modelName, ngModel) {
              self.formComponents[modelName] = ngModel;
              self.paymentControls[paymentDirectiveName] = ngModel;
            },

            tokenForm: function() {
              return {
                number: cardControl().$modelValue,
                cvc: cvcControl().$modelValue,
                exp_month: parseInt(expMonthControl().$modelValue, 10),
                exp_year: parseInt(expYearControl().$modelValue, 10)
              };
            },

            controlForErrorCode: function(errorCode) {
              return {
                "invalid_number": cardControl(),
                "incorrect_number": cardControl(),
                "card_declined": cardControl(),
                "expired_card": cardControl(),
                "processing_error": cardControl(),
                "invalid_cvc": cvcControl(),
                "incorrect_cvc": cvcControl(),
                "invalid_expiry_month": self.paymentControls[directiveNames.expiryMonth],
                "invalid_expiry_year": self.paymentControls[directiveNames.expiryYear]
              }[errorCode];
            }
          });

          var cardControl = function() { return self.paymentControls[directiveNames.number]; };
          var cvcControl = function() { return self.paymentControls[directiveNames.cvc]; };
          var expMonthControl = function () { return self.paymentControls[directiveNames.expiryMonth]; };
          var expYearControl = function () { return self.paymentControls[directiveNames.expiryYear]; };

          /**
           * Every submit should reset the form component, because its possible
           * that the error is gone, but the form is still not valid
           */
          self.resetFormComponentsValidity = function () {
            forEach(self.formComponents, function (component) {
              forEach(component.$error, function (isError, errorName) {
                if (isError && errorName.search("remote_payment_") === 0) {
                  component.$setValidity(errorName, true);
                }
              });
            });
          };

          $scope.paymentValidationError = {};
          $scope.submitted = false;

          $scope.creditCardFormSubmit = function(onSuccess) {
            log("Submitting card information form");
            $scope.onSuccess = onSuccess;

            // Hackily set the payment controls dirty just in case they were clean before;
            // error messages won't appear on controls that aren't dirty.
            forEach(self.paymentControls, function(control) {
              if (!control.$dirty) control.$setViewValue(control.$viewValue);
            });

            $scope.submitted = true;
            self.resetFormComponentsValidity();
          };

        },

        'link': function(scope, element, attrs, formCtrl) {
          // Surface the underlying FormController for access by the page's outer controller;
          // otherwise this directive's controller would have hidden it.
          scope.$parent[attrs['name']] = scope[attrs['name']];

          scope.$watch('submitted', function(submitted) {
            if (!submitted) {
              return;
            }

            payment.createToken(formCtrl.tokenForm(), function(status, response) {
              if (response.error) {
                var errorCode = response.error.code;
                var errorClass = "remote_payment_" + errorCode;
                var errorControl = formCtrl.controlForErrorCode(errorCode);
                var errorControlName = errorControl.$name;

                log(errorControl);
                log("\"" + errorCode + "\": Card information failed remote validation." +
                  " Applying angular.js validation error \"" + errorClass +
                  "\" to UI control named \"" + errorControlName + "\"");

                errorControl.$setValidity(errorClass, false);

                scope.paymentValidationError[errorControlName] = errorClass;

                $rootScope.$apply();
              } else {
                scope.onSuccess(response.id);
              }
            });

            scope.submitted = false;
          });
        }
      };
    });
  };

  // Helper for creating the input field directives that follow
  var ccFieldDirective = function(ngModule, name, validate) {
    validate = validate || function() { return true; };

    ngModule.directive(name, function() {
      return {
        'restrict': 'A',

        'require': ['^creditCardForm', 'ngModel'],

        'link': function(scope, element, attrs, ctrls) {
          var formCtrl = ctrls[0];
          var ngModel = ctrls[1];

          // Create and add the parser where applicable
          ngModel.$parsers.push(validationDirective.parser(name, ngModel, validate));

          // Add the control to the form
          formCtrl.registerFormComponent(name, attrs.name, ngModel);
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

  return {
    /** Apply to an ng.module to make payment directives available to associated page */
    applyDirectives: function(ngModule) {
      creditCardFormDirective(ngModule);
      creditCardNumberDirective(ngModule);
      creditCardCvcDirective(ngModule);
      creditCardExpiryDirectives(ngModule);
    }

  };
});
