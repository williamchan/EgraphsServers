/* Scripting for the password reset page */
/* global angular */
define(["services/forms", "Egraphs"],
function (forms, Egraphs) {

  return {
    ngControllers: {
      PasswordMatchController: ["$scope", function ($scope) {
        $scope.master = angular.copy(Egraphs.page.user);

        $scope.update = function (user) {
          $scope.master= angular.copy(user);
        };
      }]
    },

   /**
    * Executes all the scripts for the password reset template
    *
    * @return nothing
    */
    go: function () {
     $("#user_password_confirmation").change(function (e) {
       var password = $("#user_password");
       var password_confirmation = $("#user_password_confirmation");

       if(password.val() === password_confirmation.val()) {
         $("#password_message").addClass("invisible");
       } else {
         $("#password_message").removeClass("invisible");
       }
     });
    }
  };
});

