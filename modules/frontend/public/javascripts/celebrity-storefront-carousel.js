/* Scripting for the single celebrity page in carousel mode */
define(["celebrity-storefront", "Egraphs", "jquery.flexslider-min"], function(storefront, Egraphs) {
  var celebTwitterInfo = Egraphs.page.twitter_info;

  return {
    go: function() {
      storefront.populateCelebrityTweets(".recent-tweets .tweets", celebTwitterInfo);
    }
  };
});