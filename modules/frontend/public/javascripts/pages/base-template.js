/* Scripting for the base template page */
define(["bootstrap/bootstrap-modal"], function() {
  var menuStatus = "closed";

  return {
    /**
     * Executes all the scripts for the base template.
     *
     * @return nothing
     */
    go: function () {
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

        $("#signup-button").click(function () {
          $.ajax({
            url: '/subscribe',
            data: $("#signup-form").serialize(),
            type: 'post',
            success: function(data) {
              callout.text("Thanks!");
              setTimeout(function() {
                signupModal.modal('toggle');
                callout.text('');
              }, 800);
            },
            error: function () {
              callout.text("Connection error, try again later.");
              setTimeout(function() {
                signupModal.modal('toggle');
                callout.text('');
              }, 800);
            }

          });
          return false;
        });

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