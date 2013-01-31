/*global angular*/
// Configuration to look up correct urls to js files as opposed to relative
// paths (require's default behavior)
require.config({
  baseUrl: "/assets/javascripts",
  paths: {
    "bootstrap": '/assets/bootstrap/js',
    "bootstrap-modal": '/assets/bootstrap-modal/js'
  },
  urlArgs: "v=" + Egraphs.page.version
});

// The current page should have provided an array of javascript modules
// to load in the variable Egraphs.jsMain. Get those modules and load
// them by executing the 'go' method which they better have.
// If the current page has angular module dependencies,
// the page should have an array ngModules defined.

var Egraphs = Egraphs || {};
Egraphs.page = Egraphs.page || {};
Egraphs.ngModules = []; // list of angularjs modules to bootstrap onto this page
// Provide the Egraphs scope as a module to any future require() calls
define("window", [], function() { return window; });
define("Egraphs", [], function() { return Egraphs; });
define("page", [], function() { return Egraphs.page; });
define("ngModules", [], function() {
  return {
    add: function(name) {
      Egraphs.ngModules = Egraphs.ngModules.concat(name);
      return angular.module(name, []);
    }
  };
});

require(Egraphs.page.jsMain, function() {
  var mainModules = arguments,
    numModules = mainModules.length,
    i = 0,
    mainModule,
    initNgController = function(controller, name) { window[name] = controller; };

  for (i; i < numModules; i++) {
    mainModule = mainModules[i];
    mainModule.go();
    angular.forEach(mainModule.ngControllers, initNgController);
  }

  angular.element(document).ready(function() {
    angular.bootstrap(document, Egraphs.ngModules);
  });
});
