//Controllers from angular
var Controller;


//Configuration to look up correct urls to js files as opposed to relative paths (require's default behavior)
//TODO look into play plugin for require.js
//TODO organizing each page with its own main.js
require.config({
  baseUrl: "/public/javascripts"
});

//Call to template js module
require(["functions"], function(functions) {
  functions.go();
});

//Check to see if in settings
if($('#settings-page').size()) {
  require(["settings"], function(settings){
    Controller = settings.Controller;


    angular.element(document).ready(function() {
                   angular.bootstrap(document);
    });
    settings.go();
  });
}





