/* Scripting for the checkout page of the new base checkout design */
/*global angular */
define([
  "page",
  "libs/tooltip",
  "window",
  "services/logging",
  "module",
  "services/ng/payment",
  "services/ng/checkout",
  "services/responsive-modal",
  "bootstrap/bootstrap-button"
],
function(page, tooltip, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var forEach = angular.forEach;
  var celebId = page.celebId;
  var states = page.states;

  return {
    ngControllers: {
      CheckoutController: ["$scope", "$checkout", function($scope, $checkout) {
        var cartApi = $checkout.forStorefront(celebId);

        angular.extend($scope, {
          months: page.months,
          years: page.years,
          codeRedeemerVisible: false,
          states: states,
          cartApi: cartApi,
          analyticsCategory: "Checkout"
        });

        /** Toggles visibility of the discount redeeming widget */
        $scope.toggleCodeRedeemerVisibility = function() {
          $scope.codeRedeemerVisible = !$scope.codeRedeemerVisible;
          if(!$scope.codeRedeemerVisible) {
            // Toss away any entered codes since we are closing down the code-redeeming widget
            $scope.coupon.couponCode = "";
            $scope.couponForm.resource.submit();
          }
        };

        window.myScope = $scope;

        /** Refreshes $scope.cart, which tracks all of the order's line items. */
        $scope.refreshCart = function() {
          cartApi.get().success(function(cartData) {
            cartData.currentDiscount = {amount: null, status: "notApplied"};
            cartData.products = cartData.product;
            cartData.requiresShipping = false;
            forEach(cartData.products, function(product) {
              if (product.lineItemType.codeType === "PrintOrderLineItem" ) {
                cartData.requiresShipping = true;
              }
            });

            forEach(cartData.discount, function(discount) {
              cartData.currentDiscount = {amount: discount.amount, status:"applied"};
            });

            forEach(cartData.summary, function(lineItem) {
              if (lineItem.lineItemType.codeType === "TotalLineItem") {
                cartData.total = lineItem.amount;
              }
            });

            if (cartData.currentDiscount.amount) {
              $scope.codeRedeemerVisible = true;
            }

            $scope.cart = cartData;
          });
        };

        $scope.transactCheckout = function() {
          $scope.transacting = true;
          cartApi.transact().success(function(response) {
            var order = response.order;
            log("Successfully purchased. Order is: ");
            log(order);
            window.location.href = order.confirmationUrl;
          })
          .error(function(errors) {
            $scope.transacting = false;
            log("Oh snap I got some errors trying to buy this order");
            log(errors);
          });
        };

        /** Returns all forms that are relevant to the user given current cart state */
        $scope.forms = function() {
          var forms = [
            $scope.recipientForm,
            $scope.buyerForm
          ];

          if ($scope.codeRedeemerVisible) {
            forms.push($scope.couponForm);
          }

          if($scope.cart) {
            if ($scope.cart.requiresShipping) {
              forms.push($scope.shippingForm);
            }

            if ($scope.cart.total > 0) {
              forms = forms.concat([
                $scope.stripeForm,
                $scope.paymentForm
              ]);
            }

            return forms;
          }
        };

        /** Returns all user inputs relevant to the user given current cart state*/
        $scope.userControls = function() {
          var controls = [];
          forEach($scope.forms(), function(form) {
            forEach(form, function(property, propName) {
              // Go through all controls, but omit stripeToken which is not user-entered.
              if (property.$parsers && property.$name && property.$name != 'stripeToken') {
                controls.push(property);
              }
            });
          });

          return controls;
        };

        /**
         * Returns the number of fields the user still has left to enter before able
         * to validly check out
         */
        $scope.fieldsRemaining = function() {
          var controls = $scope.userControls();
          var remaining = [];
          // log("Remaining:");
          forEach(controls, function(control) {
            var fieldIsComplete = control.$valid && !control.$submitting;
            if (!fieldIsComplete) {
              // log("    " + control.$name);
              remaining.push(control);
            }
          });

          return remaining;
        };

        /**
         * Returns "readyForReview" when all forms are filled out properly.
         * Otherwise returns "enteringData"
         */
        $scope.orderStatus = function() {
          if ($scope.fieldsRemaining().length === 0) {
            return "readyForReview";
          } else {
            return "enteringData";
          }
        };

        $scope.orderCompleteIcon = function() {
          if ($scope.fieldsRemaining().length > 0 ) {
            return "glyphicons-x-circle-orange.png";
          } else {
            return "glyphicons-check-circle-dense-green.png";
          }
        };

        // Submit the payment form whenever the stripe model changes
        $scope.$watch("stripeToken.id", function(newValue, oldValue) {
          $scope.paymentForm.resource.submit();
        });

        $scope.refreshCart();

        // Make the tooltips sing the song of their people
        tooltip.apply({analyticsCategory: "Checkout"});
      }]
    }
  };
});