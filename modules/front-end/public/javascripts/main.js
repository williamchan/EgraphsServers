// Configuration to look up correct urls to js files as opposed to relative
// paths (require's default behavior)
require.config({
  baseUrl: "/assets/javascripts",
  paths: {
    "bootstrap": '/assets/bootstrap/js',
    "bootstrap-modal": '/assets/bootstrap-modal/js'
  }
});

// The current page should have provided an array of javascript modules
// to load in the variable Egraphs.jsMain. Get those modules and load
// them by executing the 'go' method which they better have.
var Egraphs = Egraphs || {};
Egraphs.page = Egraphs.page || {};
Egraphs.page.ngModules = [];
// Provide the Egraphs scope as a module to any future require() calls
define("window", [], function() { return window; });
define("Egraphs", [], function() { return Egraphs; });
define("page", [], function() { return Egraphs.page; });

require(Egraphs.page.jsMain, function() {
  var mainModules = arguments,
    numModules = mainModules.length,
    i = 0,
    mainModule;

  for (i; i < numModules; i++) {
    mainModule = mainModules[i];
    mainModule.go();
  }
  // Bootstrap angularJS. Any module dependecires should be registered like this in the appropiate javascript file
  // Egraphs.page.ngModules.push('marketplace');
  angular.element(document).ready(function() {
   angular.bootstrap(document, Egraphs.page.ngModules);
  });
});
