/* Scripting for the video assets page */
define(["Egraphs", "ngApp", "libs/angular"],
function (Egraphs, ngApp) {

  return {

    ngControllers: {

      VideoAssetCtrl: ["$scope", function ($scope) {
        $scope.videos = angular.copy(Egraphs.page.videos);
        alert("there are " + $scope.videos.length + " videos!");
      }]
    },

    go: function () {
      $(document).ready(function() {

        $("#see-unprocessed-button").click(function(e) {
          alert("ouch, you clicked me");
        });
      });
    }
  };
});