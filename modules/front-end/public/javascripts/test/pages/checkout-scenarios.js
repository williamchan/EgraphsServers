/* Testing scenarios for the checkout page in the purchase flow */
/*global angular, describe, it*/
define(
[
  "test/mock-backend",
  "services/logging",
  "module",
  "ngApp"
  ],
function(mockBackend, logging, module, ngApp) {
  var log = logging.namespace(module.id);
  var idSequence = 1;
  var BAD_REQUEST = 400;
  var newCheckout = function() {
    return {
      products: [],
      discounts: [],
      fees: [],
      summary: [{
        id: idSequence++,
        name: "Total",
        amount: 0,
        type: {
          codeType: "TotalLineItemType"
        }
      }],

      _addProduct: function(product) {
        this.products.push(product);
        this.summary[0].amount += product.amount;
      },

      _setDiscount: function(amount) {
        if (this.discounts.length > 0) {
          this.summary[0].amount += this.discounts[0].amount;
          this.discounts = [];
        }
        if (amount) {
          this.summary[0].amount -= amount;
          this.discounts.push({
            id: idSequence++,
            name: amount + " off",
            description: amount + " off",
            amount: amount,
            type: {
              codeType: "DiscountLineItemType"
            }
          });
        }
      }
    };
  };
  var checkout = newCheckout();
  var mockApi = {
    buyer: {},
    coupon: {},
    recipient: {},
    shippingAddress: {},
    payment: {}
  };
  var digitalEgraphLineItem = function() {
    return {
      id: idSequence++,
      name: "Sergio Romo egraph",
      description: "For Herp Derpson with note I'm your biggest fan!. He will sign the photo Heart of a Warrior.",
      amount: 50,
      imageUrl: "https://d3kp0rxeqzwisk.cloudfront.net/celebrity/172/profile_20120823053553898/w80.png",
      type: {
        codeType: "EgraphOrderLineItemType"
      }
    };
  };

  var framedPrintLineItem = function() {
    return {
      id: idSequence++,
      name: "Framed print",
      description: "A framed print of your digital egraph.",
      amount: 45,
      imageUrl: "/assets/images/framed-print.png",
      type: {
        codeType: "PrintOrderLineItemType"
      }
    };
  };

  var checkoutApiShouldReturn = function(checkout) {
    mockBackend.setBehavior(function($httpBackend) {
      var checkoutRegex = /checkouts\/[0-9]+$/;
      $httpBackend.whenGET(checkoutRegex).respond(checkout);
      $httpBackend.whenPOST(checkoutRegex).respond({
        "order": {
          "id": 1,
          "confirmationUrl": "/orders/1/confirm"
        }
      });
    });
  };

  var enableDiscountCodes = function(codesAndAmounts) {
    mockBackend.setBehavior(function($httpBackend) {
      $httpBackend.whenPOST(/checkouts\/[0-9]+\/coupon$/).respond(function(method, url, data) {
        var foundDiscount = false;
        var httpResult = [400, {errors:{"couponCode": ["invalid_code"]}}, {}];

        if (data.couponCode === "" || !data.couponCode) {
          httpResult = [200, "", {}];
        } else {
          angular.forEach(codesAndAmounts, function(codeAndAmount) {
            if (data.couponCode === codeAndAmount.code) {
              foundDiscount = true;
              checkout._setDiscount(codeAndAmount.amount);
              httpResult = [200, "", {}];
            }
          });
        }
        if (!foundDiscount) checkout._setDiscount(0);
        return httpResult;
      });
    });
  };

  var populateForms = function() {
    var couponCode = "15-bucks";
    enableDiscountCode(couponCode, 15);

    mockApi.coupon.couponCode = couponCode;
    mockApi.recipient.email = "recipient@egraphs.com";
    mockApi.buyer.email = "buyer@egraphs.com";
    mockApi.shippingAddress = {
      name: "Joe Recipient",
      addressLine1: "414 Rockville Way",
      addressLine2: "#401",
      city: "Needham",
      state: "CT",
      postalCode: "11111"
    };
    mockApi.payment = {
      stripeToken: "herpderp",
      postalCode: "11111"
    };
  };

  var enableDiscountCode = function(code, amount) {
    enableDiscountCodes([{code: code, amount: amount}]);
  };

  var setDiscountCodeExercised = function(code) {
    mockBackend.setBehavior(function($httpBackend) {
      $httpBackend.whenGET(/checkouts\/[0-9]+\/coupon$/).respond(function(method, url, data) {
        return [200, {couponCode: code}, {}];
      });
    });
  };

  var stubApi = function(path, propName) {
    mockBackend.stubResource({
      path: path,
      get: function(data) { return [200, mockApi[propName], {}]; },
      post: function(data) {
        mockApi[propName] = data;
        return [200, "", {}];
      }
    });
  };

  var fieldErrors = function(fieldName, errors) {
    var errorApiObjects = [];
    
    angular.forEach(errors, function(error) {
      errorApiObjects.push({field:fieldName, cause:error});
    });

    return errorApiObjects;
  };


  var acceptRecipients = function(email) {
    stubApi(/recipient/, 'recipient');
  };

  var configureDefaultCheckoutApi = function() {
    checkoutApiShouldReturn(checkout);
    stubApi(/coupon/, 'coupon');
    stubApi(/recipient/, 'recipient');
    stubApi(/buyer/, 'buyer');
    stubApi(/shipping-address/, 'shippingAddress');
    stubApi(/egraph/, 'egraph');
    stubApi(/payment/, 'payment');
  };

  return {
    "default": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());
        checkout._addProduct(framedPrintLineItem());
        enableDiscountCodes([
          {code:"15-bucks", amount:15},
          {code:"all-bucks", amount:95}
        ]);

        configureDefaultCheckoutApi();
      }
    },

    "full-discount": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());
        enableDiscountCode("herpderp", 50);

        configureDefaultCheckoutApi();
      }
    },

    
    "digital-egraph-only": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());

        configureDefaultCheckoutApi();
      }
    },

    "digital-egraph-with-framed-print": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());
        checkout._addProduct(framedPrintLineItem());

        configureDefaultCheckoutApi();
      }
    },

    "15-dollar-code-herpderp-available": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());
        enableDiscountCode("herpderp", 15);

        configureDefaultCheckoutApi();
      }
    },

    "15-dollar-code-herpderp-exercised": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());
        checkout._setDiscount(15);

        mockApi.coupon.couponCode = "herpderp";
        enableDiscountCode("herpderp", 15);

        configureDefaultCheckoutApi();
      }
    },

    "pre-populate-all-forms": {
      bootstrap: function() {
        checkout._addProduct(digitalEgraphLineItem());
        checkout._addProduct(framedPrintLineItem());

        populateForms();
        configureDefaultCheckoutApi();
      }
    },

    "all-errors": {
      bootstrap: function() {
        var notFoundResponse = [404, "", {}];
        var ok = function(data) {
          return [200, data, {}];
        };

        var badRequest = function(errors) {
          return [BAD_REQUEST, {errors:errors}, {}];
        };

        var standardErrors = ["required", "invalid_length", "invalid_format", "unexpected_type"];
        
        checkoutApiShouldReturn(checkout);
        checkout._addProduct(digitalEgraphLineItem());
        checkout._addProduct(framedPrintLineItem());

        mockBackend.stubResource({
          path: /coupon/,
          get: function() { return ok(mockApi.coupon); },
          post: function() { return badRequest({"couponCode": standardErrors.concat(["invalid_code"])});}
        });

        mockBackend.stubResource({
          path: /recipient/,
          get: function() { return ok(mockApi.recipient); },
          post: function() { return badRequest({"email": standardErrors}); }
        });

        mockBackend.stubResource({
          path: /buyer/,
          get: function() { return ok(mockApi.buyer);},
          post: function() { return badRequest({"email": standardErrors}); }
        });

        mockBackend.stubResource({
          path: /shipping-address/,
          get: function() {return ok(mockApi.shippingAddress);},
          post: function() {
            return badRequest({
              "name": ["required", "invalid_length", "invalid_format", "unexpected_type"],
              "addressLine1": standardErrors,
              "addressLine2": ["invalid_length", "invalid_format", "unexpected_type"],
              "city": standardErrors,
              "state": standardErrors,
              "postalCode": standardErrors
            });
          }
        });

        mockBackend.stubResource({
          path: /payment/,
          get: function() { return ok(mockApi.payment); },
          post: function() {
            return badRequest(
              {"postalCode": standardErrors}
            );
          }
        });

        // Populate all the forms so that once it loads it attempts to submit them again
        // and re-populates any errors.
        populateForms();

      }
    }
  };
});