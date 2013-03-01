/* Scripting for the personalize page of the new base checkout design */
/*global angular */
define([
  "page",
  "libs/tooltip",
  "services/analytics",
  "window",
  "services/logging",
  "module",
  "bootstrap/bootstrap-button",
  "services/ng/checkout",
  "services/ng/angular-strap",
  "services/ng/thumbnail-selector",
  "services/ng/resource-form"
],
function(page, tooltip, analytics, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var extend = angular.extend;
  var forEach = angular.forEach;
  var celebId = page.celebId;
  var events = analytics.eventCategory("Personalize");

  return {
    ngControllers: {
      PersonalizeController: ["$scope", "$checkout", function($scope, $checkout) {
        var cartApi = $checkout.forStorefront(celebId);
        var productsById = {};

        forEach(page.products, function(product) {
          productsById[product.id] = product;
        });

        extend($scope, {
          celebId: celebId,
          products: page.products,
          cartApi: cartApi,
          analyticsCategory: "Personalize",
          egraph: {
            isGift: "false",
            framedPrint: "false"
          }
        });

        $scope.recipientPossessive = function() {
          return $scope.egraph.isGift === "true"? "Recipient's": "Your";
        };

        $scope.recipientNominative = function() {
          if ($scope.egraph.isGift === "true" && $scope.egraph.recipientName) {
            return $scope.egraph.recipientName;
          } else {
            return "you";
          }
        };

        $scope.proceedToCheckout = function() {
          window.location.href = page.checkoutUrl;
        };

        $scope.selectedProduct = function() {
          return productsById[$scope.egraph.productId];
        };

        $scope.fieldsRemaining = function() {
          var remaining = [];
          var requiredControl = $scope.egraphForm.recipientName;

          if (!(requiredControl.$valid && !requiredControl.$submitting)) {
            remaining.push(requiredControl);
          }

          return remaining;
        };

        $scope.productPrice = function() {
          var framedPrintPrice;
          if ($scope.egraph.framedPrint === "true") {
            framedPrintPrice = 45;
          } else {
            framedPrintPrice = 0;
          }
          
          return $scope.selectedProduct().price + framedPrintPrice;
        };
      }]
    },

    go: function() {
      $(document).ready(function() {
        tooltip.apply({analyticsCategory: "Personalize"});

        $("button").click(function(event) {
          var target = $(this);
          events.track(["Button pressed", target.text().trim()]);
        });
      });
    }

  };
});