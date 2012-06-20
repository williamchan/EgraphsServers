/* Scripting for the single celebrity page in carousel mode */
define(["services/celebrity-social", "Egraphs", "libs/jquery.flexslider-min"], 
function(social, Egraphs) {
  var celebTwitterInfo = Egraphs.page.twitter_info;

  return {
    go: function() {
      social.populateCelebrityTweets(".recent-tweets .tweets", celebTwitterInfo);
      $('.flexslider').flexslider({
		slideshow: false,
		animation: "slide",
		animationDuration: 750,
		controlNav: false,
		prevText: "<span>Previous</span> &lt;",           
		nextText: "<span>Next</span> &gt;"
	  });
    }    
  };
});