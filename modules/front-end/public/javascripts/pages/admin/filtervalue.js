/* Scripting for the admin filtervalue page */
define(["Egraphs", "bootstrap/bootstrap-alert", "bootstrap/bootstrap-button", "libs/chosen/chosen.jquery.min", "services/forms"], function() {
  return {
    /**
     * Executes all the scripts for the admin filtervalue page.
     * @return nothing
     */
    go: function () {
      $('.chzn-select').chosen();
    }
  };
});