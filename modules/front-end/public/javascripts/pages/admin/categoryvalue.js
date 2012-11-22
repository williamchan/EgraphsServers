/* Scripting for the admin categoryvalue page */
define(["Egraphs", "bootstrap/bootstrap-alert", "bootstrap/bootstrap-button", "libs/chosen/chosen.jquery.min", "services/forms"], function() {
  return {
    /**
     * Executes all the scripts for the admin categoryvalue page.
     * @return nothing
     */
    go: function () {
      $('.chzn-select').chosen();
    }
  };
});