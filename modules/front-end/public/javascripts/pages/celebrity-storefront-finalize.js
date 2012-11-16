/* Scripting for the finalize page */
define(["services/forms", "services/payment", "Egraphs"],
function(forms, payment, Egraphs) {
  var thisPage = Egraphs.page.finalize;

  return {
    go: function() {
      $(document).ready(function() {
        
        // Populate the credit card field
        var $ccElem = $('.credit-card');
        if (thisPage.totalAmount === 0) {
          $ccElem.text("No charge!");

        } else {
          // Use the provided purchase token to present redacted card info.
          var paymentModule = payment[thisPage.paymentJsModule];
          paymentModule.setPublishableKey(thisPage.paymentApiKey);
          paymentModule.getToken(thisPage.paymentToken, function(status, response) {
            if (status === 200) {
              var card = response.card;
              $ccElem.text(card.type + ": ************" + card.last4);
            } else {
              $ccElem.text("Error retrieving your payment info. Please contact support.");
            }
          });
        }
      });

      // Mixpanel events
      mixpanel.track_forms('.container form', 'Purchase clicked');
    }
  };
});
