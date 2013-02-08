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
var Egraphs = Egraphs || {};
Egraphs.page = Egraphs.page || {};
Egraphs.config = Egraphs.config || {};

// Angular services defined in our require modules should add themselves
// to the Egraphs angular module by calling .factory, .directive, etc upon
// this injectable module.
Egraphs.config.ngApp = angular.module("Egraphs", []);
Egraphs.config.ngModules = [];

// Provide a bunch of Egraphs scope vars as require modules
define("window", [], function() { return window; });
define("Egraphs", [], function() { return Egraphs; });
define("page", [], function() { return Egraphs.page; });
define("ngApp", [], function() { return Egraphs.config.ngApp; });
define("ngModules", [], function() {
  return {
    add: function(name, dependencies) {
      Egraphs.config.ngModules.push(name);
      return angular.module(name, dependencies);
    }
  };
});
define("config", [], function() { return Egraphs.config; });

require(Egraphs.page.jsMain, function() {
  var mainModules = arguments,
    numModules = mainModules.length,
    i = 0,
    mainModule,
    scenarios,
    noop = angular.noop,
    initNgController = function(controller, name) { window[name] = controller; },
    bootstrapAngular = function(onBootstrapped) {
      angular.element(document).ready(function() {
        angular.bootstrap(document.body, Egraphs.config.ngModules.concat("Egraphs"));
        $("body").addClass('ng-app'); // Necessary to allow the test runner to do its thing
        (onBootstrapped || noop)();
      });
    };

  for (i; i < numModules; i++) {
    mainModule = mainModules[i];
    if (typeof(mainModule.go) === "function") { mainModule.go(); }
    angular.forEach(mainModule.ngControllers, initNgController);
  }

  if (Egraphs.page.scenarios && Egraphs.page.scenarios.testcase) {
    scenarios = Egraphs.page.scenarios;
    require([scenarios.module, "services/logging"], function (pageScenarios, logging) {
      var log = logging.namespace("main.js");
      var testcase = pageScenarios[scenarios.testcase];
      var bootstrap = testcase.bootstrap || noop;
      var documentReady = function() {
        (testcase.documentReady || noop)();
      };

      log("Running scenario " + scenarios.module + "#" + scenarios.testcase);

      bootstrap();
      bootstrapAngular(documentReady);
    });
  } else {
    bootstrapAngular();
  }
});
