/* Module provides different payment service implementations for usage in production and testing
 */
define(["page", "libs/stripe-v1"],
function(page) {
  var stripePayment = window.Stripe;
  var paymentConfig = page.paymentConfig;
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

      if (paymentConfig.errorCode) {
        error = {
          code: paymentConfig.errorCode
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
  if (paymentConfig) {
    moduleName = paymentConfig.jsModule;
    apiKey = paymentConfig.apiKey;

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
    throw "No payment configuration provided by page. Egraphs.page.paymentConfig.jsModule and .apiKey are required";
  }
});