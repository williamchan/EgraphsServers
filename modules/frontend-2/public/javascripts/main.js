// Configuration to look up correct urls to js files as opposed to relative
// paths (require's default behavior)
require.config({
  baseUrl: "/public/javascripts",
  paths: {
    bootstrap: '/public/twitter-bootstrap/js'
  }
});

// The current page should have provided an array of javascript modules
// to load in the variable Egraphs.jsMain. Get those modules and load
// them by executing the 'go' method which they better have.
var Egraphs = Egraphs || {};
Egraphs.page = Egraphs.page || {};

// Provide the Egraphs scope as a module to any future require() calls
define("Egraphs", [], function() { return Egraphs; });

require(Egraphs.page.jsMain, function() {
  var mainModules = arguments,
    numModules = mainModules.length,
    i = 0,
    mainModule;

  for (i; i < numModules; i++) {
    mainModule = mainModules[i];
    mainModule.go();
  }
});
