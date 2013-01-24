/* Scripting for the base template page */
define(["page", "window", "services/logging", "module", "bootstrap/bootstrap-modal"],
  function(page, window, logging, requireModule) {
  var menuStatus = "closed";
  var log = logging.namespace(requireModule.id);
  var mailerController = function($scope, $http) {
    var mail = page.mail;
    $scope.mailer = {};
    angular.extend($scope.mailer, {email_address : "", apikey: mail.apikey, id: mail.id,  double_optin: false, method: "listSubscribe"});
    $scope.message = "Join our mailing list.";

    $scope.subscribe = function() {
      log($scope.mailer);
      $http({
        method: 'POST',
        url: mail.url,
        data: $scope.mailer
      }).success( function() {
        log("Subscribed!");
        $scope.message = "Thank you!";
        mixpanel.track('Subscribed to newsletter');
      }).error( function() {
        log("Error!");
        $scope.message = "Sorry, something went wrong. Try again later.";
      });
    };
  };

  return {
    /**
     * Executes all the scripts for the base template.
     *
     * @return nothing
     */
    go: function () {

      window.MailerController = mailerController;
      angular.element(document).ready(function() {
        angular.bootstrap(document, []);
      });

      $(document).ready(function(){
        var callout = $("#signup-callout");
        var signupModal = $('#emailSignupForm');
        // highlight action on top menu
        $('#top .account').hover(function(){
          $(this).addClass('hover');
        }, function(){
          $(this).removeClass('hover');
        });

        // show top account menu on click
        $('#top .account.logged-in').click(function(e){
          if (menuStatus === "closed") {
            var account_options = $(this).find('.account-options');
            $('body').one('click',function(){
              account_options.removeClass('active');
              menuStatus = "closed";
            });
            account_options.addClass('active');
            menuStatus = "open";
            e.stopPropagation();
            e.preventDefault();
          }
        });

        // smooth scroll to top
        $('.to-top a').click(function(e){
            $('html,body').animate({scrollTop: $($(this).attr('href')).offset().top},'slow');
            e.preventDefault();
        });

        $("#email-link").click(function(e) {
          signupModal.modal('toggle');
          $('html,body').animate({scrollTop: $($(this).attr('href')).offset().top},'slow');
          e.preventDefault();
        });

        // $("#signup-button").click(function () {
        //   $.ajax({
        //     url: '/subscribe',
        //     data: $("#signup-form").serialize(),
        //     type: 'post',
        //     statusCode : {
        //       200: function(data) {
        //         callout.text("Thanks!");
        //         setTimeout(function() {
        //           signupModal.modal('toggle');
        //           callout.text('');
        //         }, 800);
        //       },
        //       400: function () {
        //         callout.text("That's not an email address =/");
        //       },
        //       500: function () {
        //         callout.text("Connection error, try again later.");
        //         setTimeout(function() {
        //           signupModal.modal('toggle');
        //           callout.text('');
        //         }, 800);
        //       }
        //     }

        //   });
        //   mixpanel.track('Subscribed to newsletter');
        //   return false;
        // });

        // set modal to visible if toggled.
        if(Egraphs.page.modalOn === true) {
          $(window).load(function(){
              signupModal.modal({
              });
          });
        }
      });
    }
  };
});