/* Scripting for the gift certificates checkout page */

/*global angular */
define([
  "services/forms",
  "services/payment",
  "services/ng/payment",
  "page",
  "window",
  "services/logging",
  "module",
  "services/responsive-modal"
],
function(forms, payment, ngPayment, page, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var forEach = angular.forEach;

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
  forEach(page.certificateOptions, function(option) {
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

    $scope.onCardInfoValidated = function(cardToken) {
      log("Woot! success");
      log($scope);
    };

    $scope.printScope = function() {
      log($scope);
    };

    /**
     * Iterates through all errors of all forms, and removes any that began with "remote-"
     * from the controls to which they applied.
     */
    var clearRemoteErrors = function() {
      forEach(forms(), function(form) {
        forEach(form.$error, function(controlsWithErrors, errorName) {
          if (errorName.search("remote_") === 0) {
            forEach(controlsWithErrors, function(controlWithErrors) {
              controlWithErrors.$setValidity(errorName, true);
            });
          }
        });
      });
    };

    var formsValid = function() {
      return !($scope.ownAmount.$invalid || $scope.pay.$invalid || $scope.personalize.$invalid);
    };

    var dirtyUserControls = function() {
      forEach(userControls(), function(control) {
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
        $scope.personalize.email,
        $scope.pay.postalCode
      ];
    };

    var forms = function() {
      return [$scope.ownAmount, $scope.personalize, $scope.pay];
    };

    // Select the default option
    forEach(certificateOptions, function(option) {
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