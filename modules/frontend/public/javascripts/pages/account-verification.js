/* Scripting for the verification page */
define(["Egraphs", "libs/angular"], function (Egraphs) {
  var Controller = function ($scope) {
    $scope.master = angular.copy(Egraphs.page.user)

    $scope.update = function (user) {
      $scope.master= angular.copy(user);
    };
  }
  return {
  /**
   * Executes all the scripts for the verification template
   *
   * @return nothing
   */
    go: function () {
      window.Controller = Controller;
      angular.element(document).ready(function () {
        angular.bootstrap(document);
     });

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
  }
});

