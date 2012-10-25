/* Scripting for the landing page */
define(["bootstrap/bootstrap-tooltip", "bootstrap/bootstrap-popover"],
function () {
  return {
    go: function() {

      $(".soldout-tooltip").tooltip({placement:"top"});

      // Mixpanel events
      mixpanel.track('Home page viewed', {'query': window.location.search })
      mixpanel.track_links('#get-started-button', 'Get Started clicked');
      $(".celebrities figure>a").click(function() {
        mixpanel.track("Celebrity clicked");
      });
      $(".celebrities h4>a").click(function() {
        mixpanel.track("Celebrity clicked");
      });

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