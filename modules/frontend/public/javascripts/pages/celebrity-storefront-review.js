/* Scripting for the review page */
define(["services/forms", "Egraphs"],
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