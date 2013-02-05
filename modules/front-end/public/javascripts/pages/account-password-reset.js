/* Scripting for the password reset page */
define(["Egraphs"], function (Egraphs) {
  var Controller = function ($scope) {
    $scope.master = angular.copy(Egraphs.page.user);

    $scope.update = function (user) {
      $scope.master= angular.copy(user);
    };
  };
  return {
  /**
   * Executes all the scripts for the password reset template
   *
   * @return nothing
   */
    go: function () {
      window.Controller = Controller;

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

