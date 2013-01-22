/**
 * Configurable payment service implementations for usage in production and testing.
 * The module follows the interface described in the stripe documentation:
 * https://stripe.com/docs/stripe.js.
 *
 * The serving page must configure this module by adding the following configurations to
 * window.Egraphs.page:
 *
 *   angular.extend(Egraphs.page, {
 *     payment: {
 *       apiKey: "jq_98139813", // REQUIRED. Public API Key for payment partner (stripe).
 *       jsModule: "stripe-payment" // Optional. "stripe-payment" (default), "yes-maam-payment".
 *       errorCode: "incorrect_cvc" // Optional. If running "yes-maam-payment", calls to createToken
 *                                  // will respond with this error as per the stripe.js
 *                                  // documentation.
 *     }
 *   });
 */
define(["page", "window", "libs/stripe-v1"],
function(page, window) {
  var stripePayment = window.Stripe;
  var config = page.payment;
  var apiKey;
  var moduleName;
  var configuredModule;

  /** A bald-faced stub implementation of stripe.js. See https://stripe.com/docs/stripe.js */
  var yesMaamPayment = {
    setPublishableKey: function(lol) {
      // I don't cryptographically secure anything. I'm about as safe
      // as unprotected sex.
    },

    createToken: function(configObj, handler) {
      // My payment implementation on the server doesn't care what I
      // generate here; it'll always response "Yes ma'am".
      var error;

      if (config.errorCode) {
        error = {
          code: config.errorCode
        };
      }

      window.setTimeout(
        function(){ handler(200, {id:"A Token...not that it matters", error: error});},
        100
      );
    },

    getToken: function(tokenId, callback) {
      // Just a copy-paste of a sample stripe token
      var fakeStripeToken = {"amount":0,"created":1341466397,"currency":"usd","id":"tok_fsVvVZQxxtbXEl","livemode":false,"object":"token","used":false,"card":{"country":"US","exp_month":4,"exp_year":2015,"fingerprint":"jaW3rHjVmI2YwHq9","last4":"4242","object":"card","type":"Visa"}};
      callback(200, fakeStripeToken);
    },

    validateCVC: function(cvc) {
      return true;
    },

    validateExpiry: function(month, year) {
      return true;
    },

    validateCardNumber: function(number) {
      return true;
    }
  };

  // Allow the page to set payment implementation if it wants
  if (config) {
    moduleName = config.jsModule;
    apiKey = config.apiKey;

    if (moduleName === "stripe-payment") {
      configuredModule = stripePayment;
    } else if (moduleName === "yes-maam-payment") {
      configuredModule = yesMaamPayment;
    } else {
      configuredModule = stripePayment;
    }

    configuredModule.setPublishableKey(apiKey);

    return configuredModule;
  } else {
    throw "No payment configuration provided by page. Egraphs.page.payment.apiKey is required";
  }
});