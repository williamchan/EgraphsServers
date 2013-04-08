/* Scripting for the brandstore*/
define(
["page",
 "services/logging",
 "services/analytics",
 "module",
 "bootstrap/bootstrap-typeahead"],
function(page, logging, analytics, requireModule) {
  var log = logging.namespace(requireModule.id);
  var events = analytics.eventCategory("Brandstore");

  return {
    // ngControllers: {},

    go: function () {
      $(document).ready(function(){
        $(".typeahead").typeahead(
          {
            source: ['Boston Red Sox', 'San Francisco Giants', 'Houston Astros', 'Miami Marlins', 'Texas Rangers', 'Milwaukee Brewers']
          }
        );
    });
  }
};});