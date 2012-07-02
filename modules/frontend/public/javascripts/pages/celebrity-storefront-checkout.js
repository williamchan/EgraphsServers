/* Scripting for the checkout page */
define(["services/forms", "Egraphs", "libs/chosen/chosen.jquery.min"],
function(forms, Egraphs) {

  return {
    go: function() {
      $(document).ready(function() {
        forms.setAlert('.alert');
        $(".chsn-select").chosen({no_results_text: "No results matched"});

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