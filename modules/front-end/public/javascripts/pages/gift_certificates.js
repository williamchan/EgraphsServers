/* Scripting for the gift certificates checkout page.
 *
 * Note that this is a half-complete implementation. It currently correctly validates and
 * accepts all payment info and ships it off to our payment partner, but nothing further.
 *
 * When we want to finish this, a controller will have to be written to accept the payment info
 * as well as to incrementally validate.
 *
 */
/*global angular */
define([
  "services/ng/payment",
  "page",
  "window",
  "services/logging",
  "module",
  "services/responsive-modal"
],
function(ngPayment, page, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var forEach = angular.forEach;

  // Helper type that adds some richer data on top of the certificate objects injected by the
  // page.
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

  // Populate the array of certificate options ($25, $50, etc)
  var certificateOptions = [];

  forEach(page.certificateOptions, function(option) {
    certificateOptions.push(new GiftCertificateOption(option));
  });

  // For use in non-production
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

  return {

    ngControllers: {
      GiftCertificatePurchaseController: function ($scope) {
        var config = page.config;

        // Bang some page configuration into the $scope
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

        // Select the default option
        forEach(certificateOptions, function(option) {
          if (option.isDefault) $scope.selectOption(option);
        });
      }
    },

    go: function() {
      $(".modify-order").click(function() {
        $("#review").responsivemodal("toggle");
      });
    }
  };
});