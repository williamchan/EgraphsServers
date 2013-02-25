/* Scripting for the egraph classic page */
define(["Egraphs"], function (Egraphs) {
  return {
    go: function () {
      // Mixpanel events
      mixpanel.track('Egraph viewed', {'url': window.location.href});
      $('.vjs-big-play-button').click(function() {
        mixpanel.track("Egraph video played");
      });
      mixpanel.track_links('#promo-shop-link', 'Shop button clicked from egraph');
      mixpanel.track_links('.pin-it-button', 'Pin It button clicked');
      mixpanel.track_links('#classic-link', 'Classic egraph mode clicked');
    }
  };
});
