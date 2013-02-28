/* Scripting for the admin mastheads page */
define(["page", "window", "services/logging", "module", "libs/chosen/chosen.jquery.min", "services/forms"],
  function(page, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);

  return {
    go: function () { 
        $('.chzn-select').chosen();
    }
  };
});
