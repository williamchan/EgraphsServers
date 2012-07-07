/* Scripting for the checkout page */
define(["services/forms", "services/payment", "Egraphs", "libs/chosen/chosen.jquery.min"],
function(forms, payment, Egraphs) {
  // Get information from the page
  var checkout = Egraphs.page.checkout;
  var paymentModule = payment[checkout.paymentJsModule];

  /** Retrieves the DIV that holds nondescript errors. */
  var $generalErrorsInputDiv = function() {
    return $(".general-errors");
  };

  /**
   * Controller that manages UI state during Stripe payment dynamics. It requires a bunch
   * of jquery selectors that match up with div.input tags that represent our form fields.
   *
   * @param $card div.input for the card number
   * @param $cvc div.input for the card cvc
   * @param $expiry div.input that contains both expiration date <select>s
   * @param $form a selector containing the actual form itself
   */
  var PaymentController = function($card, $cvc, $expiry, $submitButton, $form) {
    var allFields = [$card, $cvc, $expiry, $submitButton];

    /** Clears all errors associated with payment */
    var clearAllErrors = function() {
      var i = 0;
      setFieldError($generalErrorsInputDiv(), false, "");
      for(i; i < allFields.length; i++) {
        setFieldError(allFields[i], false, "");
      }
    };

    /** Gets the text value of one of the div.inputs */
    var textFieldValue = function($inputDiv) {
      return $inputDiv.find("input").val();
    };

    /** Gets the expiration dates as a string array of [month, year] */
    var expiryDates = function() {
      return $expiry.find("select").map(function() {
        return $(this).val();
      });
    };

    /** Gets the entered in card number */
    var cardNumber = function () {
      return textFieldValue($card);
    };

    /** Gets the entered in CVC */
    var cardCvc = function () {
      return textFieldValue($cvc);
    };

    /**
     * Sets or removes an error on a payment field
     *
     * @param inputDiv the div on which to manipulate the error state. e.g. $card.
     * @param isError true that we should be setting rather than unsetting the error
     * @param errorMessage message to put into the error field
     */
    var setFieldError = function($inputDiv, isError, errorMessage) {
      var $fieldErrorDiv = $inputDiv.find(".alert-error");
      if (isError) {
        $inputDiv.addClass("error");
        $fieldErrorDiv.removeClass("hidden");
      } else {
        $inputDiv.removeClass("error");
        $fieldErrorDiv.addClass("hidden");
      }
      
      $inputDiv.find("span.error-message").text(errorMessage);
    };

    /** Handles response from the stripe api. */
    var paymentResponseHandler = function(status, response) {
      clearAllErrors();

      if (response.error) {
        var targetAndMessage = targetAndMessageForError(response.error);
        setFieldError(targetAndMessage.target, true, targetAndMessage.message);
        $submitButton.removeAttr("disabled");
      } else {
        // Ok we've got a token. Let's submit it.
        $form.append(
          "<input type='hidden' name='order.billing.token' value='" +
          response.id +
          "'/>"
        );
        $form.get(0).submit();
      }
    };

    /**
     * Gets the field target and message for a stripe error
     *
     * @return {target: (some input div, such as $card), message: (some error message string)}
     */
    var targetAndMessageForError = function(error) {
      var paramTarget = errorParameterToDomElement[error.param] || defaultErrorParameterDomElement;
      var message = errorCodeToMessage[error.code];

      return {
        target: paramTarget.element,
        message: message || paramTarget.defaultMessage
      };
    };

    /** Maps stripe API error codes to human-readable message */
    var errorCodeToMessage = {
      "invalid_number": "Invalid card number",
      "incorrect_number": "Invalid card number" ,
      "invalid_expiry_month": "Invalid expiration date",
      "invalid_expiry_year": "Invalid expiration date",
      "invalid_cvc": "Invalid CVC",
      "expired_card": "This card is expired",
      "incorrect_cvc": "Security code not accepted",
      "card_declined": "This card was declined"
    };

    /**
      * Maps stripe API error parameters to inputDiv selectors on our form and default
      * error messages for those form elements.
      */
    var errorParameterToDomElement = {
      "exp_year": {element:$expiry, defaultMessage: "Enter an expiration date"},
      "exp_month": {element:$expiry, defaultMessage: "Enter an expiration date"},
      "cvc": {element:$cvc, defaultMessage: "Invalid CVC"},
      "number": {element: $card, defaultMessage: "Invalid card number"}
    };
    var defaultErrorParameterDomElement = {
      element: $generalErrorsInputDiv(),
      defaultMessage: "There was an issue with our payment processor. Isn't it worth another shot?"
    };


    /**
     * Binds the controller onto handlers in the dom and on its services.
     */
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