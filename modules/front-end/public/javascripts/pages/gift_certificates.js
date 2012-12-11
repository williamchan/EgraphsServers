/* Scripting for the gift certificates checkout page */
define(["services/forms", "services/payment", "Egraphs", "libs/chosen/chosen.jquery.min", "bootstrap/bootstrap-modal"],
function(forms, payment, Egraphs) {
  return { 
    go: function() { 
      console.log("Herp derp");
      $(".chsn-select").chosen({no_results_text: "No results matched"});
      // $("#review").modal("toggle")
    }
  };
});