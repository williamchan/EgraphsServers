/** 
 * "Yes Ma'am" client-side payment implementation. Mocks the Stripe 1.0 library
 * and generates fake tokens for consumption by services.payment.YesMaamPayment,
 * which should never _ever_ be configured during production.
 */
define([], function() {
  return {
    setPublishableKey: function(lol) {
      // I don't cryptographically secure anything. I'm about as safe
      // as unprotected sex.
    },

    createToken: function(configObj, handler) {
      // My payment implementation on the server doesn't care what I
      // generate here; it'll always response "Yes ma'am".
      handler(200, {id:"A Token...not that it matters"});
    }
  };
});