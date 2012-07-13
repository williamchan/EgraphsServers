/* Scripting for the review page */
define(["services/forms", "Egraphs", "bootstrap/bootstrap-modal"],
function(forms, Egraphs) {

  return {
    go: function() {
      forms.setIphoneCheckbox('#order-print', { 
      	checkedLabel: 'YES', 
      	uncheckedLabel: 'NO'
      });
    }    
  };
});