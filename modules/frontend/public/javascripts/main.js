//Controllers from angular
var Controller;

//Configuration to look up correct urls to js files as opposed to relative paths (require's default behavior)
require.config({
  baseUrl: "/public/javascripts"
});

//Call to template js module
require(["functions"], function(functions) {
  functions.go();
});
//Load page specific javascript
var Egraphs = Egraphs || {};

require([Egraphs.jsMain], function(module) {
  module.go();
})






