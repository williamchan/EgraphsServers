/* Scripting for the gift certificates checkout page */

/*global angular */
define([
  "services/forms",
  "services/payment",
  "Egraphs",
  "libs/chosen/chosen.jquery.min",
  "services/responsive-modal"],
function(forms, payment, Egraphs) {
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
  angular.forEach(Egraphs.page.certificateOptions, function(option) {
    certificateOptions.push(new GiftCertificateOption(option));
  });

  var GiftCertificatePurchaseController = function ($scope) {
    $scope.certificateOptions = certificateOptions;

    $scope.selectOption = function(certificateOption) {
      // Toggle selected state
      if ($scope.selectedOption !== certificateOption) {
        certificateOption.select(true);
        if ($scope.selectedOption) $scope.selectedOption.select(false);
        $scope.selectedOption = certificateOption;
      }
    };

    // Select the default option
    angular.forEach(certificateOptions, function(option) {
      if (option.isDefault) $scope.selectOption(option);
    });
  };

  return {
    go: function() {
      window.GiftCertificatePurchaseController = GiftCertificatePurchaseController;

      $(".chsn-select").chosen({no_results_text: "No results matched"});

      $("#review-button").click(function() {
        $("#review").responsivemodal("toggle");
      });

      $(".modify-order").click(function() {
        $("#review").responsivemodal("toggle");
      });

      angular.element(document).ready(function() {
        angular.bootstrap(document);
      });
    }
  };
});