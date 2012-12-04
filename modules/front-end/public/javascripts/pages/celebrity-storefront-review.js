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

      $('.iPhoneCheckContainer').click(function() {
        if (!alertedRegardingHolidayPrintDelivery && $('#order-print')[0].checked === true) {
          alertedRegardingHolidayPrintDelivery = true;
          alert("Just making sure you know that this will not arrive by December 24. \n\nStill want the framed print? \n\n:-)");
        }
      });


      // Mixpanel events
      mixpanel.track_forms('.container form', 'Checkout clicked');
    }
  };
});