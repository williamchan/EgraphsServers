/* Module provides different payment service implementations for usage in production and testing
 */
define(["libs/stripe-v1"],
function() {
  /** A bald-faced mock implementation of stripe.js. See https://stripe.com/docs/stripe.js */
  var yesMaamPayment = {
    setPublishableKey: function(lol) {
      // I don't cryptographically secure anything. I'm about as safe
      // as unprotected sex.
    },

    createToken: function(configObj, handler) {
      // My payment implementation on the server doesn't care what I
      // generate here; it'll always response "Yes ma'am".
      handler(200, {id:"A Token...not that it matters"});
    },

    getToken: function(tokenId, callback) {
      // Just a copy-paste of a sample stripe token
      var fakeStripeToken = {"amount":0,"created":1341466397,"currency":"usd","id":"tok_fsVvVZQxxtbXEl","livemode":false,"object":"token","used":false,"card":{"country":"US","exp_month":4,"exp_year":2015,"fingerprint":"jaW3rHjVmI2YwHq9","last4":"4242","object":"card","type":"Visa"}};
      callback(200, fakeStripeToken);
    }
  };

  return {
    "stripe-payment": window.Stripe,
    "yes-maam-payment": yesMaamPayment
  };
});