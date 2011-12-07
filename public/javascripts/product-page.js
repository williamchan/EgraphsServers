/* Scripting for the product page, e.g. /Wizzle/2010-Starcraft-Tournament */
define(["stripe"], function(stripe) {
  /**
   * Handles submission of a create token request to Stripe.
   */
  var stripeResponseHandler = function(status, response)  {
    if (response.error) {
      console.log(response);

      // Show the errors on the form
      displayMessageForError(response.error);

      // Enable re-submission
      enableSubmitButton(true);
    } else {
        var form$ = $("#payment-form");
        // token contains id, last4, and card type
        var token = response['id'];
        // insert the token into the form so it gets submitted to the server
        form$.append("<input type='hidden' name='stripeToken' value='" + token + "'/>");
        // and submit
        form$.get(0).submit();
    }
  };

  /**
   * Presents an error message on the page given a Stripe error.
   */
  var displayMessageForError = function(error) {
    var message = null;
    var param = error.param;
    var code = error.code;
    
    if (param === "number" || code === "invalid_number" || code === "incorrect_number") {
      message = "The provided card number is invalid.";
    } else if (param === "exp_year" || 
			         param === "exp_month" ||
               code === "invalid_expiry_month" || 
               code === "invalid_expiry_year") {
	    message = "The expiration date was rejected.";
    } else if (param === "cvc" ||  code === "invalid_cvc" || code === "incorrect_cvc") {
      message = "The provided CVC security code was incorrect.";
    } else if (code === "expired_card") {
      message = "The provided card has expired";
    } else if (code === "card_declined") {
      message = "The provided card has been declined";
    }

    if (message) {
      message = message + " Please fix and re-submit.";
    } else {
      message = "There was an error processing your card. Please try again later.";
    }

    $("#form-errors").html(message).css("display", "block");
  };

  /**
   * Enables and disables the submit button, to prevent double-submission.
   */
  var enableSubmitButton= function(enable) {
    var button = $('#submit-button');
    if (enable) {
      button.removeAttr("disabled");
    } else {
      button.attr("disabled", "true");
    }
  };

  return {
    /**
     * Executes all the scripts for the product page.
     *
     * @param stripeKey the Stripe publishable key for payments.
     * @param productPriceInCents the price in cents of the product being displayed.
     *
     * @return nada
     */
    go: function (stripeKey, productPriceInCents) {
      stripe.setPublishableKey(stripeKey);

      $(document).ready(function() {
        $("#payment-form").submit(function(event) {
          // disable the submit button to prevent repeated clicks
          enableSubmitButton(false)

          var amount = productPriceInCents; //amount you want to charge in cents
          Stripe.createToken({
              number: $('#cardNumber').val(),
              cvc: $('#cardCvc').val(),
              exp_month: $('#cardExpirationMonth').val(),
              exp_year: $('#cardExpirationYear').val()
          }, amount, stripeResponseHandler);

          // prevent the form from submitting with the default action
          return false;
        });
      });
    }
  }
});