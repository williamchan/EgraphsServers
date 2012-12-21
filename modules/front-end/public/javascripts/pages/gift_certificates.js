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

  var GiftCertificatePurchaseController = function ($scope) {
    $scope.certificateOptions = certificateOptions;
    $scope.months = page.months;
    $scope.years = page.years;

    $scope.selectOption = function(certificateOption) {
      // Toggle selected state
      if ($scope.selectedOption !== certificateOption) {
        certificateOption.select(true);
        if ($scope.selectedOption) $scope.selectedOption.select(false);
        $scope.selectedOption = certificateOption;
      }
    };

    $scope.submitForReview = function() {
      if (formsInvalid) {
        dirtyUserControls();
      } else {
        $("#review").responsivemodal("toggle");
      }
    };

    var formsInvalid = function() {
      return $scope.ownAmount.$invalid || $scope.pay.$invalid || $scope.personalize.invalid;
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