/* Scripting for the egraph classic page */
define(["Egraphs", "services/analytics"], function (Egraphs, analytics) {
  var events = analytics.eventCategory("Egraph");
  var social = events.social;
  var $playButton = function() { return $('.vjs-big-play-button'); };

//  Egraph.page.logging.level = "all";

  return {
    go: function () {
      $(document).ready(function() {
        // Mixpanel events
        mixpanel.track('Egraph viewed', {'url': window.location.href});
        mixpanel.track_links('#promo-shop-link', 'Shop button clicked from egraph');
        mixpanel.track_links('#pin-it-img', 'Pin It button clicked');
        mixpanel.track_links('#classic-link', 'Classic egraph mode clicked');
        $playButton().click(function() {
          mixpanel.track("Egraph video played");
        });

        //
        // Google Analytics events
        //
        $('#egraph-player').click(function(e){
          // includes if $playButton is clicked, take difference for clicks on egraph outside of play button
          events.track("Playback", "Egraph player clicked");
        });

        $playButton().click(function(e) {
          events.track("Playback", "Big play button clicked");
        });

        $('#promo-shop-link').click(function(e){
          events.track('Promo', 'Get Your Own clicked');
        });

        $('#classic-link').click(function(e){
          events.track('Classic', "Visited from new egraph");
        });

        //
        // Facebook Events
        // https://developers.google.com/analytics/devguides/collection/gajs/gaTrackingSocial#facebook
        FB.Event.subscribe('edge.create', social.fb.like.track);
        FB.Event.subscribe('edge.remove', social.fb.unlike.track);
        FB.Event.subscribe('message.send', social.fb.share.track);

        //
        // Twitter Events
        // https://developers.google.com/analytics/devguides/collection/gajs/gaTrackingSocial#twitter
        var tweetTracker = social.twitter.tweet.track;
        var bindTweetTracker = function(twttr) { twttr.events.bind('tweet', tweetTracker) };
        twttr.ready(bindTweetTracker);

        //
        // Pinterest events
        //
        $('#pin-it-img').click(function(e){
          social.pinterest.pin.track();
        });
      });
    }
  };
});
