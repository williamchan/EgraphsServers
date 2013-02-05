/* Scripting for the base template page */
define(["page", "window", "services/logging", "module", "services/ng/mail-services", "services/responsive-modal"],
  function(page, window, logging, requireModule) {
  var menuStatus = "closed";
  var log = logging.namespace(requireModule.id);

  var mailerController = function($scope, subscribeService) {
    $scope.email = "";
    $scope.message = "Join our mailing list.";
    $scope.subscribe = function() {
      subscribeService($scope.email,
        function() { $scope.message = "Thanks!";},
        function() { $scope.message = "Sorry, there was an error. Try again later.";}
      );
    };
  };

  var modalController = function($scope, subscribeService) {
    $scope.email = "";
    $scope.subscribe = function() {
      subscribeService($scope.email);
      $('#emailSignupForm').modal('toggle');
    };
  };

  // Define dependencies.
  mailerController.$inject = ['$scope', 'subscribe'];
  modalController.$inject = ['$scope', 'subscribe'];
  window.MailerController = mailerController;
  window.ModalController =  modalController;

  return {

    ngModules: ['MailServices'],

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

        var mobileNavOut = false;
        var pushedElements = $("header, footer, section");
        var mobileMenu = $("#left-flyout");
        log("Binding things");
        $(".navbar-expand").click(function() {
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
        if(Egraphs.page.modalOn === true) {
          $(window).load(function(){
            signupModal.modal({});
          });
        }
      });
    }
  };
});