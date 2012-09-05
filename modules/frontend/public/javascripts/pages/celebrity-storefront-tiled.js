/* Scripting for the single celebrity page in tiled mode */
define(["services/celebrity-social", "Egraphs"],
function(social, Egraphs) {
  var celebTwitterInfo = Egraphs.page.twitter_info;

  return {
    go: function() {
      social.populateCelebrityTweets(".recent-tweets .tweets", celebTwitterInfo);

      // Mixpanel events
      mixpanel.track_links('article div>a', 'Product clicked');
      mixpanel.track_links('article h2>a', 'Product clicked');
    }
  };
});
