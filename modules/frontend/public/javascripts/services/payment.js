define(["libs/stripe-v1"],
function() {
  var yesMaamPayment = {
    setPublishableKey: function(lol) {
      // I don't cryptographically secure anything. I'm about as safe
      // as unprotected sex.
    },

    createToken: function(configObj, amount, handler) {
      // My payment implementation on the server doesn't care what I
      // generate here; it'll always response "Yes ma'am".
      handler(200, {id:"A Token...not that it matters"});
    }
  };

  return {
    "stripe-payment": window.Stripe,
    "yes-maam-payment": yesMaamPayment
  };
});