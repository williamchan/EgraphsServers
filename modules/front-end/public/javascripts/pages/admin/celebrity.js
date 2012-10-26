/* Scripting for the admin celebrity page */
define(["Egraphs", "libs/chosen/chosen.jquery.min", "services/forms"], function (Egraphs) {
    return {
      /**
      * Executes all the scripts for the admin template. 
      * @return nothing
      */
      go: function () { 
        $('.chzn-select').chosen();
	  }
	}
  }
);
