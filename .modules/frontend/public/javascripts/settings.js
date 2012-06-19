/* Scripting for the settings page */
define([], function () {
  //Controller class for Angular app
  var Controller = function ($scope) {
  //Plugin default values here
    $scope.master = { fullname : "Joshua Johnson",
                      username : "joshuajohnson",
                      email : "joshua12@gmail.com",
                      address :
                        { title : "Default",
                          fullname : "Joshua Johnson",
                          line1 : "5507 N 58th Street",
                          line2 : "",
                          city : "Seattle",
                          zip : "98101",
                          state : "WA"
                        },
                      gallery : {
                        visibility : "Visible",
                        url : "egr.aphs/joshuaj"
                      },
                      notices : [{ text: "New Star Additions (Weekly)",
                                   name: "new_stars",
                                   value: true},
                                 { text: "New Products/Features",
                                   name: "news",
                                   value: false},
                                 { text: "Updates from Major League Baseball",
                                   name: "mlb_updates",
                                   value: true}
                                ]
                      };

    $scope.update = function (user) {
      $scope.master = angular.copy(user);
    };

    $scope.reset = function () {
      $scope.user = angular.copy($scope.master);
    }

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
    }

    $scope.reset();

    $scope.cancel = function (modelString, parentID) {
      var parent = $("#" + parentID);
      parent.addClass('hide');
      parent.prev().removeClass('hide');
      $scope.resetVal(modelString);
    }
  }

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

          thisRow.addClass('hide');
          thisRow.next().removeClass('hide');
          e.preventDefault();
        });

        $("#user_password_confirmation").change(function (e) {
          var password = $("#user_password");
          var password_confirmation = $("#user_password_confirmation");

          if(password.val() === password_confirmation.val()) {
            $("#password_message").addClass("invisible");
          } else {
            $("#password_message").removeClass("invisible");
          }
//            console.log($(this).val());
        });

      });
    }
}});