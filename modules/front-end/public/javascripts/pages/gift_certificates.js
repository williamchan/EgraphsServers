/* Scripting for the gift certificates checkout page */

/*global angular */
define([
  "services/forms",
  "services/payment",
  "services/ng/payment",
  "page",
  "services/responsive-modal"],
function(forms, payment, ngPayment, page) {
  var GiftCertificateOption = function(data) {
    this.price = data.price;
    this.type = data.type;
    this.isMostPopular = data.isMostPopular;
    this.isConcrete = data.type === "concrete";
    this.isCustom = data.type === "custom";
    this.isDefault = data.isDefault;
    this.buttonSpanSize = this.isConcrete? "span2": "span4";

    this.select(false);
  };

  GiftCertificateOption.prototype.select = function(select) {
    this.selected = select;
    this.buttonClass = select? "active": "";
    this.amountFieldClass = select? "scrolled-out": "scrolled-in";
  };

  var certificateOptions = [];
  angular.forEach(page.certificateOptions, function(option) {
    certificateOptions.push(new GiftCertificateOption(option));
  });

  var module = angular.module('giftCertificatePurchaseApp', []);
  ngPayment.applyDirectives(module);

  var sampleData = {
    order: {
      recipientName: "Herp Derpson",
      gifterName: "Derp Herpson",
      email: "derpson@egraphs.com",
      card: {
        name: "Herp Derpson Sr",
        number: "4242424242424242",
        expMonth: "01",
        expYear: "2015",
        cvc: "123",
        postalCode: "12345"
      }
    }
  };

  var GiftCertificatePurchaseController = function ($scope) {
    var config = page.config;
    var paymentToken;

    angular.extend($scope, {
      certificateOptions: certificateOptions,
      months: page.months,
      years: page.years
    });

    if (config.useSampleData) angular.extend($scope, sampleData);

    $scope.selectOption = function(certificateOption) {
      // Toggle selected state
      if ($scope.selectedOption !== certificateOption) {
        certificateOption.select(true);
        if ($scope.selectedOption) $scope.selectedOption.select(false);
        $scope.selectedOption = certificateOption;
      }
    };

    $scope.submitForReview = function() {
      clearRemoteErrors();
      dirtyUserControls();
      if (!config.validateClientSide || formsValid()) {
        var paymentParams = {
          number: $scope.order.card.number,
          cvc: $scope.order.card.cvc,
          exp_month: parseInt($scope.order.card.expMonth, 10),
          exp_year: parseInt($scope.order.card.expYear, 10)
        };

        payment.createToken(paymentParams, function(status, response) {
          // Do some more validation on stripe response and then call against OUR apis.
          if (response.error) {
            var errorCode = response.error.code;
            var control = stripeErrorCodesToControls()[errorCode];
            control.$setValidity("remote-stripe-" + errorCode, false);
            // $("#review").responsivemodal("loading");
            // $("#review").responsivemodal("toggle");
          } else {
            paymentToken = response.id;

            // We'll have to pass the buck here too
            // $("#review").responsivemodal("loading");
          }
        });
      }
    };

    /**
     * Iterates through all errors of all forms, and removes any that began with "remote-"
     * from the controls to which they applied.
     */
    var clearRemoteErrors = function() {
      angular.forEach(forms(), function(form) {
        angular.forEach(form.$error, function(controlsWithErrors, errorName) {
          if (errorName.search("remote-") === 0) {
            angular.forEach(controlsWithErrors, function(controlWithErrors) {
              controlWithErrors.$setValidity(errorName, true);
            });
          }
        });
      });
    };

    /**
     * Returns an object that maps stripe error codes to the respective
     * controls to which the error would apply.
     */
    var stripeErrorCodesToControls = function() {
      return {
        "invalid_number": $scope.pay.cardNumber,
        "incorrect_number": $scope.pay.cardNumber,
        "card_declined": $scope.pay.cardNumber,
        "expired_card": $scope.pay.cardNumber,
        "processing_error": $scope.pay.cardNumber,
        "invalid_cvc": $scope.pay.cardCvc,
        "incorrect_cvc": $scope.pay.cardCvc,
        "invalid_expiry_month": $scope.pay.expMonth,
        "invalid_expiry_year": $scope.pay.expYear
      };
    };

    var formsValid = function() {
      return !($scope.ownAmount.$invalid || $scope.pay.$invalid || $scope.personalize.$invalid);
    };

    var dirtyUserControls = function() {
      angular.forEach(userControls(), function(control) {
        // Hackily set dirty flag on all user controls to
        // enable error styling even on previously un-touched
        // inputs.
        control.$setViewValue(control.$viewValue);
      });
    };

    var userControls = function() {
      return [
        $scope.ownAmount.amount,
        $scope.personalize.recipientName,
        $scope.personalize.gifterName,
        $scope.pay.cardName,
        $scope.pay.email,
        $scope.pay.cardNumber,
        $scope.pay.cardExpMonth,
        $scope.pay.cardExpYear,
        $scope.pay.cardCvc,
        $scope.pay.postalCode
      ];
    };

    var forms = function() {
      return [$scope.ownAmount, $scope.personalize, $scope.pay];
    }

    // Select the default option
    angular.forEach(certificateOptions, function(option) {
      if (option.isDefault) $scope.selectOption(option);
    });
  };

  return {
    go: function() {
      window.GiftCertificatePurchaseController = GiftCertificatePurchaseController;

      $(".modify-order").click(function() {
        $("#review").responsivemodal("toggle");
      });

      angular.element(document).ready(function() {
        angular.bootstrap(document, ['giftCertificatePurchaseApp']);
      });
    }
  };
});