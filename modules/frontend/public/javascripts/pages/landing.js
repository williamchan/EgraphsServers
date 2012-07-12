/* Scripting for the landing page */
define([], function () {
  return {
    go: function() {
      // Set up featured stars
      var landing_celebrities_btn = $('#landing-stars h3 a');
      var landing_celebrities = $('#landing-stars .celebrities');

      //landing_celebrities.hide().css('opacity', '0');

      landing_celebrities_btn.hover(function(){
        $(this).parent().animate({ top: '-80px' }, 200);
      }, function() {
        $(this).parent().animate({ top: '-75px' }, 200);
      });

      if (screen.width > 480) {
          landing_celebrities_btn.toggle(function(e)
          { landing_celebrities.animate({ opacity: 0 }).slideUp('fast');
            e.preventDefault();
          },function(e){
            landing_celebrities.slideDown('fast').animate({ opacity: 1 });
            e.preventDefault();
          });
      } else {
        landing_celebrities_btn.click(function(e){
          $('html,body').animate({scrollTop: $($(this).attr('href')).offset().top},'slow');
          e.preventDefault();
        });
      }
    }
  };
});