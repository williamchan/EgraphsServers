/* Scripting for the landing page */
define(["bootstrap/bootstrap-tooltip", "bootstrap/bootstrap-popover", "bootstrap/bootstrap-tab", "bootstrap/bootstrap-carousel"],
function () {
  return {
    go: function() {
      $(document).ready(function() {
        $(".soldout-tooltip").tooltip({placement:"top"});

        // Mixpanel events
        mixpanel.track('Home page viewed', {'query': window.location.search });
        mixpanel.track_links('#get-started-button', 'Get Started clicked');
        $(".celebrities figure>a").click(function() {
          mixpanel.track("Celebrity clicked");
        });
        $(".celebrities h4>a").click(function() {
          mixpanel.track("Celebrity clicked");
        });

        $(".switcher-tab").click(function(e){
          var tab = $(this);
          var parent = tab.parent();
          parent.addClass("active");
          parent.siblings().removeClass("active");

          $(".tab").hide();
          $(tab.attr("href")).fadeIn();
          e.preventDefault();
        });

        $("a.helper").click(function() {
          $("#learn-switch").click();
        });


        //$('[id^="myCarousel"]').carousel();

      });
    }
  };
});