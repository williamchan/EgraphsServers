/* Scripting for the checkout page */
define(["services/forms", "services/payment", "Egraphs", "libs/chosen/chosen.jquery.min"],
function(forms, payment, Egraphs) {
  var checkout = Egraphs.page.checkout;
  var paymentModule = payment[checkout.paymentJsModule];

  var $generalErrorsInputDiv = function() {
    return $(".general-errors");
  };

  var PaymentController = function($card, $cvc, $expiry, $submitButton, $form) {
    var allFields = [$card, $cvc, $expiry, $submitButton];

    var clearAllErrors = function() {
      var i = 0;
      setFieldError($generalErrorsInputDiv(), false, "");
      for(i; i < allFields.length; i++) {
        setFieldError(allFields[i], false, "");
      }
    };

    var textFieldValue = function($inputDiv) {
      return $inputDiv.find("input").val();
    };

    var expiryDates = function() {
      return $expiry.find("select").map(function() {
        return $(this).val();
      });
    };

    var cardNumber = function () {
      return textFieldValue($card);
    };

    var cardCvc = function () {
      return textFieldValue($cvc);
    };

    var setFieldError = function($inputDiv, isError, errorMessage) {
      $fieldErrorDiv = $inputDiv.find(".alert-error");
      if (isError) {
        $inputDiv.addClass("error");
        $fieldErrorDiv.removeClass("hidden");
      } else {
        $inputDiv.removeClass("error");
        $fieldErrorDiv.addClass("hidden");
      }
      
      $inputDiv.find("span.error-message").text(errorMessage);
    };

    var paymentResponseHandler = function(status, response) {
      clearAllErrors();

      if (response.error) {
        var targetAndMessage = targetAndMessageForError(response.error);
        setFieldError(targetAndMessage.target, true, targetAndMessage.message);
        $submitButton.removeAttr("disabled");
      } else {
        // Ok we've got a token. Let's submit it.
        $form.append("<input type='hidden' name='stripeTokenId' value='" + response.id + "'/>");
        $form.get(0).submit();
      }
    };

    var targetAndMessageForError = function(error) {
      var paramTarget = errorParameterToDomElement[error.param];
      var message = errorCodeToMessage[error.code];

      return {
        target: paramTarget.element || $generalErrorsInputDiv(),
        message: message || paramTarget.defaultMessage || "Unknown error processing payments"
      };
    };

    var errorCodeToMessage = {
      "invalid_number": "Invalid card number",
      "incorrect_number": "Invalid card number" ,
      "invalid_expiry_month": "Invalid expiration date",
      "invalid_expiry_year": "Invalid expiration date",
      "invalid_cvc": "Invalid CVC",
      "expired_card": "This card is expired",
      "incorrect_cvc": "Security code not accepted",
      "card_declined": "This card was declined",
      "processing_error": "There was an issue with our payment processor. Isn't it worth another shot?"
    };

    var errorParameterToDomElement = {
      "exp_year": {element:$expiry, defaultMessage: "Enter an expiration date"},
      "exp_month": {element:$expiry, defaultMessage: "Enter an expiration date"},
      "cvc": {element:$cvc, defaultMessage: "Invalid CVC"},
      "number": {element: $card, defaultMessage: "Invalid card number"}
    };


    this.bind = function() {
      $form.submit(function(event) {
        try {
          var expiration = expiryDates();
          var tokenParams = {
            number: cardNumber(),
            cvc: cardCvc(),
            exp_month: parseInt(expiration[0], 10),
            exp_year: parseInt(expiration[1], 10)
          };
          
          paymentModule.createToken(tokenParams, checkout.productPriceInCents, paymentResponseHandler);
          
          // Stop the form form submitting
          return false;
        } finally {
          return false;
        }
      });
    };
  };
  
  return {
    go: function() {
      $(document).ready(function() {
        // Enable payments
        paymentModule.setPublishableKey(checkout.paymentApiKey);
        var $cardNumberInputDiv = $(".input.stripe-card-number");
        var $cvcInputDiv = $(".input.card-cvc");
        var $expiryInputDiv = $(".input.card-expiry");
        var $submitButton = $(".checkout-submit");
        var $form = $("#checkout-form");
        var paymentController = new PaymentController(
          $cardNumberInputDiv,
          $cvcInputDiv,
          $expiryInputDiv,
          $submitButton,
          $form
        );
        
        paymentController.bind();

        // Enable alert warnings
        forms.setAlert('.alert');

        // Enable chosen.js style selectors
        $(".chsn-select").chosen({no_results_text: "No results matched"});

        // Hook into enable/disable of billing
        if ($("#billing-same").is(":checked")) $("#billing-info").hide();
        
        // checkbox functionality (show/hide alternate billing)
        $("#billing-same").click(function() {
          if ($(this).is(":checked")) {
            $("#billing-info").fadeOut();
          } else {
            $("#billing-info").fadeIn().find("input:first").focus();
          }
        });
      });
    }
  };
  
});