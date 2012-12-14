/* Scripting for the gift certificates checkout page */
define([
  "services/forms",
  "services/payment",
  "Egraphs",
  "libs/chosen/chosen.jquery.min",
  "services/responsive-modal"],
function(forms, payment, Egraphs) {
  return {
    go: function() {
      $(".chsn-select").chosen({no_results_text: "No results matched"});

      $("#review-button").click(function() {
        $("#review").responsivemodal("toggle");
      });

      $("#modify-order").click(function() {
        $("#review").responsivemodal("toggle");
      });
    }
  };
});