/* Scripting for the base template page */
define([], function() {

  return {
    /**
     * Executes all the scripts for the base template.

     * @return nothing
     */
    go: function () {
      $(document).ready(function(){
        // highlight action on top menu
        $('#top .account').hover(function(){
          $(this).addClass('hover');
        }, function(){
          $(this).removeClass('hover');
        });

        // show top account menu on click
        $('#top .account').click(function(e){
          var account_options = $(this).find('.account-options');
          $('body').one('click',function(){
            account_options.removeClass('active');
          });
          account_options.addClass('active');
          e.stopPropagation();
          e.preventDefault();
        });

        // smooth scroll to top
        $('.to-top a').click(function(e){
            $('html,body').animate({scrollTop: $($(this).attr('href')).offset().top},'slow');
            e.preventDefault();
        });


        var landing_celebrities_btn = $('#landing-stars h3 a');
        var landing_celebrities = $('#landing-stars .celebrities');

        landing_celebrities.hide().css('opacity', '0');

        landing_celebrities_btn.hover(function(){
          $(this).parent().animate({ top: '-80px' }, 200);
        }, function() {
          $(this).parent().animate({ top: '-75px' }, 200);
        });

        if (screen.width > 480) {
            landing_celebrities_btn.toggle(function(e){
              landing_celebrities.slideDown('fast').animate({ opacity: 1 });
              e.preventDefault();
            }, function(e) {
              landing_celebrities.animate({ opacity: 0 }).slideUp('fast');
              e.preventDefault();
            });
        } else {
          landing_celebrities_btn.click(function(e){
            $('html,body').animate({scrollTop: $($(this).attr('href')).offset().top},'slow');
            e.preventDefault();
          });
        }
      });
    }
  };
});