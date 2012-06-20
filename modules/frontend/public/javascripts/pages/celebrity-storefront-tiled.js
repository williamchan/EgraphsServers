/* Scripting for the single celebrity page in tiled mode */
define(["services/celebrity-social", "Egraphs"], 
function(social, Egraphs) {
  var celebTwitterInfo = Egraphs.page.twitter_info;

  return {
    go: function() {
      social.populateCelebrityTweets(".recent-tweets .tweets", celebTwitterInfo);
    }
  };
});