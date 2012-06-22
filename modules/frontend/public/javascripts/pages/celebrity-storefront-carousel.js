/* Scripting for the single celebrity page in carousel mode */
define(["services/celebrity-social", "Egraphs", "libs/flexslider/jquery.flexslider-min"], 
function(social, Egraphs) {
  var page = Egraphs.page,
      celebTwitterInfo = page.twitter_info,
      firstSlideIndex = page.firstSlideInCarousel;

  return {
    go: function() {
      social.populateCelebrityTweets(".recent-tweets .tweets", celebTwitterInfo);
      $('.flexslider').flexslider({
        slideshow: false,
        animation: "slide",
        animationDuration: 750,
        controlNav: false,
        slideToStart: firstSlideIndex,
        prevText: "<span>Previous</span> &lt;",
        nextText: "<span>Next</span> &gt;"
	    });
    }    
  };
});