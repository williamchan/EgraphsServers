/* Scripting for the finalize page */
define(["services/forms", "services/payment", "Egraphs"],
function(forms, payment, Egraphs) {
  var thisPage = Egraphs.page.finalize;
  var paymentModule = payment[thisPage.paymentJsModule];

  return {
    go: function() {
      $(document).ready(function() {
        // Populate the credit card field
        paymentModule.setPublishableKey(thisPage.paymentApiKey);
        paymentModule.getToken(thisPage.paymentToken, function(status, response) {
          var $ccElem = $('.credit-card');
          if (status === 200) {
            var card = response.card;
            $ccElem.text(card.type + " ending in " + card.last4);
          } else {
            $ccElem.text("Error retrieving your payment info. Please contact support.");
          }
        });
      });
    }
  };
});
