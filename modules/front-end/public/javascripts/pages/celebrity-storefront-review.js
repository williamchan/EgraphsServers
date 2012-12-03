/* Scripting for the review page */
define(["services/forms", "Egraphs", "bootstrap/bootstrap-modal"],
function(forms, Egraphs) {

  var alertedRegardingHolidayPrintDelivery = false;

  return {
    go: function() {
      forms.setIphoneCheckbox('#order-print', {
      	checkedLabel: 'YES', 
      	uncheckedLabel: 'NO'
      });

      $('.iPhoneCheckHandleCenter').click(function() {
        if (!alertedRegardingHolidayPrintDelivery && $('#order-print')[0].checked === true) {
          alertedRegardingHolidayPrintDelivery = true;
          alert("Framed prints ordered now will NOT arrive by December 24.");
        }
      });


      // Mixpanel events
      mixpanel.track_forms('.container form', 'Checkout clicked');
    }
  };
});