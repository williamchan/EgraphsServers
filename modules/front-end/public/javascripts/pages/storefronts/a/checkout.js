/* Scripting for the checkout page of the new base checkout design */
/*global angular */
define([
  "page",
  "libs/tooltip",
  "window",
  "services/analytics",
  "services/logging",
  "module",
  "services/ng/payment",
  "services/ng/checkout",
  "services/ng/resource-form",
  "services/responsive-modal",
  "bootstrap/bootstrap-button"
],
function(page, tooltip, window, analytics, logging, requireModule) {
  var log = logging.namespace(requireModule.id);
  var forEach = angular.forEach;
  var celebId = page.celebId;
  var states = page.states;
  var events = analytics.eventCategory("Checkout");

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
          analyticsCategory: "Checkout",
          egraph: {}
        });

        cartApi.egraph().success(function(egraph) {
          $scope.egraph = egraph;
        });

        /** Toggles visibility of the discount redeeming widget */
        $scope.toggleCodeRedeemerVisibility = function() {
          $scope.codeRedeemerVisible = !$scope.codeRedeemerVisible;
          if(!$scope.codeRedeemerVisible) {
            // Toss away any entered codes since we are closing down the code-redeeming widget
            $scope.coupon = {couponCode:""};
            $scope.couponForm.resource.submit();
            events.track(["Code Redeemer - Close"]);
          } else {
            events.track(["Code Redeemer - Open"]);
          }
        };

        $scope.codeRedeemerText = function() {
          if ($scope.codeRedeemerVisible) {
            return "remove code";
          } else {
            return "redeem code";
          }
        };

        window.myScope = $scope;

        /** Refreshes $scope.cart, which tracks all of the order's line items. */
        $scope.refreshCart = function() {
          var durationEvent = events.startEvent(["Cart updated"]);

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

            
            if (cartData.products.length) {
              $scope.cart = cartData;
              durationEvent.track();
            } else {
              /** 
               * Cannot be on checkout page unless there is something in the cart; normally,
               * protected by getCheckout endpoint, but forward button can subvert it.
               */
              window.location.href = page.personalizeUrl;
            }
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
          .error(function(data, status) {
            var errors;
            $scope.transacting = false;

            if (status === 400) {
              errors = data.errors;
              if (errors.payment) {
                $scope.stripeForm.stripe.setErrors(errors.payment.concat("stripe_transaction"));
              }

              if (errors.egraph) {
                $scope.errors = {noInventory:true};
              }
            } else if (status === 500) {
              $scope.errors = {serverError:true};
              log("Server error. Not much we can do about it...");
            }
          });
        };

        /** Returns all forms that are relevant to the user given current cart state */
        $scope.forms = function() {
          var forms = [
            $scope.buyerForm
          ];

          if ($scope.egraph.isGift === "true") {
            forms.push($scope.recipientForm);
          }

          // Only include the coupon form in relevant forms if it's visible and has a value filled.
          if ($scope.codeRedeemerVisible && ($scope.coupon && $scope.coupon.couponCode !== "")) {
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

        $scope.submitting = function() {
          var submitting = false;
          forEach($scope.forms(), function(form) {
            if (form.$submitting) {
              submitting = true;
            }
          });

          return submitting;
        };

        /**
         * Returns the number of fields the user still has left to enter before able
         * to validly check out
         */
        $scope.fieldsRemaining = function() {

          /**
           * Hacky fix to remaining field count being wrong. Difficult, with my limited knowledge, to otherwise set the
           * state of the name field of the buyer form to a valid state in a more legit way. If this is not done, the
           * remaining fields count is one higher than it should be until the buyer form is submitted with a valid email.
           */
          if ($scope.egraph.isGift == "false") {
            $scope.buyerForm.name.$valid = true;
          }

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
          if ($scope.fieldsRemaining().length === 0 && !$scope.errors) {
            return "readyForReview";
          } else {
            return "enteringData";
          }
        };

        $scope.orderCompleteIcon = function() {
          if ($scope.fieldsRemaining().length > 0 && !$scope.errors) {
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