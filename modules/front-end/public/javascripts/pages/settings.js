/* Scripting for the settings page */
/*global angular*/
define(["Egraphs", "services/logging", "module", "services/analytics"],
function (Egraphs, logging, module, analytics) {
  var log = logging.namespace(module.id);
  var mailEvents = analytics.eventCategory("Mail");

  var isSubscribedToNewsletter = function() {
    var checkbox = $("[name=notices\\.new_stars]")[0];
    if (!checkbox) return false;
    else return checkbox.checked;
  };

  return {
    ngControllers: {
      Controller: function ($scope) {
        //Plugin default values here
        $scope.master = angular.copy(Egraphs.page.user);

        $scope.update = function (user) {
          $scope.master = angular.copy(user);
        };

        $scope.reset = function () {
          $scope.user = angular.copy($scope.master);
        };

        // Resets an individual value in a form by doing a lookup for the model reference.
        $scope.resetVal = function (val) {
          var splitpath = val.split(".");
          var master = $scope.master;
          var userObj = $scope.user;
          var i;
          // Get second to last element to change the reference to the original master string.
          for(i = 1; i < splitpath.length - 1; i++){
            log(splitpath[i]);
            master = master[splitpath[i]];
            userObj = userObj[splitpath[i]];
          }

          userObj[splitpath[splitpath.length - 1]] = angular.copy(master[splitpath[splitpath.length - 1]]);
        };

        $scope.cancel = function (modelString, parentID) {
          var parent = $("#" + parentID);
          parent.addClass('none');
          parent.prev().removeClass('none');
          $scope.resetVal(modelString);
        };

        $scope.reset();
      }
    },

    /**
     * Executes all the scripts for the settings template.
     * @return nothing
     */
    go: function () {
      var initiallySubscribedToNewsletter = false;

      // Bindings for edit buttons
      $(document).ready(function () {

        $(".edit").click(function(e){
          var thisRow = $(this);

          thisRow.addClass('none');
          thisRow.next().removeClass('none');
          e.preventDefault();
        });

        $("#account-settings-form").submit( function(e) {
          var justSignedUpForNewsletter = !initiallySubscribedToNewsletter && isSubscribedToNewsletter();
          if (justSignedUpForNewsletter) {
            mailEvents.track(['Subscribed to newsletter', 'Account settings']);
          }
          return true;
        });
      });

      $(window).load(function() {
        initiallySubscribedToNewsletter = isSubscribedToNewsletter();
      });
    }
  };
});
