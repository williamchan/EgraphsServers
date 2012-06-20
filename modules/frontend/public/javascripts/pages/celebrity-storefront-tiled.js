/* Scripting for the single celebrity page in carousel mode */
define(["services/celebrity-social", "Egraphs"], 
function(social, Egraphs) {
  var celebTwitterInfo = Egraphs.page.twitter_info;

  return {
    go: function() {
      social.populateCelebrityTweets(".recent-tweets .tweets", celebTwitterInfo);
    }
  };
});