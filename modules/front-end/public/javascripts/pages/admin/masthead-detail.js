/* Scripting for the base template page */
define(["page", "window", "services/logging", "module"],
  function(page, window, logging, requireModule) {
  var log = logging.namespace(requireModule.id);

  return {
    ngControllers: {
      MastheadController: ['$scope', function($scope) {
        $scope.masthead = angular.copy(Egraphs.page.masthead);
      }
    ]},

    go: function () {

    }
  };
});