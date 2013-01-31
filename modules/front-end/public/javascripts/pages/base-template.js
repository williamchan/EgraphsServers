/* Scripting for the base template page */
define(
["page",
 "window",
 "services/logging",
 "module",
 "services/ng/mail-services",
 "bootstrap/bootstrap-modal"],
function(page, window, logging, requireModule) {
  var menuStatus = "closed";
  var log = logging.namespace(requireModule.id);

  return {
    ngControllers: {

      /** Controller for mail signup at bottom-right of site template */
      MailerController: ['$scope', '$subscribe', function($scope, $subscribe) {
        $scope.email = "";
        $scope.message = "Join our mailing list.";
        $scope.subscribe = function() {
          $subscribe($scope.email,
            function() { $scope.message = "Thanks!";},
            function() { $scope.message = "Sorry, there was an error. Try again later.";}
          );
        };
      }],

      /**
       * Controller for mail signup via the modal template occasionally available during email
       * signup drives.
       */
      ModalController: ['$scope', '$subscribe', function($scope, $subscribe) {
        $scope.email = "";
        $scope.subscribe = function() {
          $subscribe($scope.email);
          $('#emailSignupForm').modal('toggle');
        };
      }]
    },

    go: function () {
      $(document).ready(function(){
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

        // set modal to visible if toggled.
        if(page.modalOn === true) {
          $(window).load(function(){
              signupModal.modal({});
          });
        }

        // Populate social links
        require(["services/social-links"], function(links) {
          links.insertIntoPage();
        });
      });
    }
  };
});