// Configuration to look up correct urls to js files as opposed to relative
// paths (require's default behavior)
require.config({
  baseUrl: "/public/javascripts"
});

// The current page should have provided an array of javascript modules
// to load in the variable Egraphs.jsMain. Get those modules and load
// them by executing the 'go' method which they better have.
var Egraphs = Egraphs || {};

require(Egraphs.jsMain, function() {
  var mainModules = arguments,
    numModules = mainModules.length,
    i = 0,
    mainModule;

  for (i; i < numModules; i++) {
    mainModule = mainModules[i];
    mainModule.go();
  }
});






