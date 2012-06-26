/* Scripting for the checkout page */
define(["services/forms", "Egraphs"],
function(forms, Egraphs) {

  return {
    go: function() {
      forms.setAlert('.alert');
    }    
  };
  
});