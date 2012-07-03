/* Scripting shared between both celebrity storefront pages */
define(["libs/jquery.tweet"], function() {
  return {
    /**
     * Populates a celebrity's tweets inside of the given selector
     * @param selector the selectors to fill with the celebrity's tweets
     * @param celebTwitterInfo an object with a .name and .handle field, that correspond
     *   to the celebrity's plain name and twitter handle.
     **/
    populateCelebrityTweets: function(selector, celebTwitterInfo) {
	  $(".recent-tweets .tweets").tweet({
		username: celebTwitterInfo.handle,
		filter: function(t){ 
			var matches = /^@\w+/.test(t['tweet_raw_text']);
			return !matches; 
		},
		avatar_size: 40,
		fetch: 20, // the last twenty tweets should contain at least 3 that are not @replies ;-)
		count: 3,
		loading_text: "loading...",
		template: "<!-- {avatar} --> <span class='tweet-heading'><strong>" +
                   celebTwitterInfo.name +
                   "</strong> @" + 
                   celebTwitterInfo.handle +
                   "</span> {text} {time}"
	  });
    }
  };
});