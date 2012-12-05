/* Scripting for the settings page */
define(["Egraphs", "libs/angular", "services/forms"], function (Egraphs) {
  //Controller class for Angular app
  var Controller = function ($scope) {
    //Plugin default values here
    $scope.master = angular.copy(Egraphs.page.user);

    $scope.update = function (user) {
      $scope.master = angular.copy(user);
    };

    $scope.reset = function () {
      $scope.user = angular.copy($scope.master);
    };

    //Resets an individual value in a form by doing a lookup for the model reference.
    $scope.resetVal = function (val) {
      var splitpath = val.split(".");
      var master = $scope.master;
      var userObj = $scope.user;
      //Get second to last element to change the reference to the original master string.
      for(i = 1; i < splitpath.length - 1; i++){
        console.log(splitpath[i]);
        master = master[splitpath[i]];
        userObj = userObj[splitpath[i]];
      }

      userObj[splitpath[splitpath.length - 1]] = angular.copy(master[splitpath[splitpath.length - 1]]);
    };

    $scope.reset();

    $scope.cancel = function (modelString, parentID) {
      var parent = $("#" + parentID);
      parent.addClass('none');
      parent.prev().removeClass('none');
      $scope.resetVal(modelString);
    };
  };

  return {
    /**
     * Executes all the scripts for the settings template.

     * @return nothing
     */
    go: function () {
      window.Controller = Controller;

      angular.element(document).ready(function() {
        angular.bootstrap(document);
      });
      //Bindings for edit buttons
      $(document).ready(function () {

        $(".edit").click(function(e){
          var thisRow = $(this);

          thisRow.addClass('none');
          thisRow.next().removeClass('none');
          e.preventDefault();
        });

//        $("#user_password_confirmation").change(function (e) {
//          var password = $("#user_password");
//          var password_confirmation = $("#user_password_confirmation");
//
//          if(password.val() === password_confirmation.val()) {
//            $("#password_message").addClass("invisible");
//          } else {
//            $("#password_message").removeClass("invisible");
//          }
//        });

      });
    }
  };
});
