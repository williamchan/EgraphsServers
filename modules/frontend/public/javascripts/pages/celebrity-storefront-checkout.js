/* Scripting for the checkout page */
define(["services/forms", "Egraphs"],
function(forms, Egraphs) {

  return {
    go: function() {
    
      forms.setAlert('.alert');

			if ($("#billing-same").is(":checked")) $("#billing-info").hide();
				
			// checkbox functionality (show/hide alternate billing)
			$("#billing-same").click(function() {
				if ($(this).is(":checked")) {
					$("#billing-info").hide();
				} else {
					$("#billing-info").show().find("input:first").focus();					
				}
			});

    }    
  };
  
});