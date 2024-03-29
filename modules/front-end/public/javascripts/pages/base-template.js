/* Scripting for the base template page */
define(
["page",
 "window",
 "services/logging",
 "services/analytics",
 "module",
 "libs/tooltip",
 "services/ng/mail-services",
 "services/responsive-modal"],
function(page, window, logging, analytics, requireModule) {
  var menuStatus = "closed";
  var log = logging.namespace(requireModule.id);
  var events = analytics.eventCategory("Base");

  return {
    ngControllers: {

      /** Controller for mail signup at bottom-right of site template */
      MailerController: ['$scope', '$subscribe', function($scope, $subscribe) {
        $scope.email = "";
        $scope.message = "Join our mailing list for great deals and news.";
        $scope.subscribe = function() {
          $subscribe($scope.email,
            function() { $scope.message = "Aaaand you're signed up. Welcome to the club!";},
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
          //show modal
          e.preventDefault();
        });

        var mobileNavOut = false;
        // Use the .flyout-pull-right class to select any elements outside of the
        // the standard #top, #bottom, #content containers that must slide out and away
        // from the mobile navigation menu affectionately referred to as the "burger bar."

        var pushedElements = $("#top, #bottom, #content, .flyout-pull-right");
        var mobileMenu = $("#left-flyout");
        log("Binding things");
        $(".navbar-expand, .btn-navbar").click(function() {
          log("click");
          if(mobileNavOut === false) {
            log("expanding");
            pushedElements.addClass("left-flyout-push");
            mobileMenu.addClass("is-visible");
            mobileNavOut = true;
          } else {
            log("contracting");
            pushedElements.removeClass("left-flyout-push");
            mobileMenu.removeClass("is-visible");
            mobileNavOut = false;
          }
        });

        // set modal to visible if toggled.
        if(page.modalOn === true) {
          $(window).load(function(){
            signupModal.modal({});
          });
        }

        signupModal.on('shown', function() {
          events.track(['Newsletter modal shown']);
        });

        signupModal.on('hidden', function() {
          events.track(['Newsletter modal hidden']);
        });

        // Populate social links asynchronously
        require(["services/social-links"], function(links) {
          links.insertIntoPage();
        });
      });
    }
  };
});